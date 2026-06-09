# talent-gateway 实现说明

> 模块路径：`talent-ai-backend/talent-gateway`  
> 端口：**8080**  
> 定位：整个后端微服务的**唯一对外 HTTP 入口**

---

## 一、它解决什么问题？

在没有网关时，前端要分别记住多个地址：

```
8081  talent-auth      /api/auth/**
8082  talent-job        /api/job/**
8083  talent-resume     /api/resume/**
8084  talent-ai-agent   /api/ai/**
8085  talent-interview  /api/interview/**
```

每个服务还要各自做：登录校验、跨域（CORS）、路由转发——非常分散。

**talent-gateway 的作用**可以概括为三点：

| 职责 | 说明 |
|------|------|
| **统一入口** | 前端只访问 `http://localhost:8080`，由网关按 URL 前缀转发到对应微服务 |
| **统一鉴权** | 在网关层校验 JWT Token，下游服务不必重复解析 Token |
| **统一跨域** | 浏览器从前端（5173）调 API 时的 CORS 在网关一处配置 |

```
┌─────────────┐     /api/**      ┌──────────────────┐     lb://服务名     ┌─────────────┐
│ talent-ai-  │ ───────────────► │  talent-gateway  │ ─────────────────► │ talent-auth │
│ front:5173  │   代理到 8080    │      :8080       │   Nacos 发现实例   │ talent-job  │
└─────────────┘                  └──────────────────┘                    │ ...         │
                                                                        └─────────────┘
```

当前端 `vite.config.ts` 里配置了：

```ts
proxy: { '/api': { target: 'http://localhost:8080' } }
```

所以用户浏览器里看到的是 `localhost:5173/api/...`，实际请求会被 Vite 转到网关 8080，再由网关转到具体微服务。

---

## 二、模块有多「轻」？

整个 gateway 模块**只有 5 个文件**，没有 Controller、没有 Service、没有数据库：

```
talent-gateway/
├── pom.xml
└── src/main/
    ├── java/com/talent/gateway/
    │   ├── GatewayApplication.java      # 启动类
    │   └── filter/
    │       └── AuthGlobalFilter.java    # 全局 JWT 鉴权（核心逻辑）
    └── resources/
        ├── bootstrap.yml                # Nacos 注册与配置
        └── application.yml              # 端口、路由、CORS
```

网关**不写业务**，只做「交通警察 + 门卫」。

---

## 三、技术栈与依赖（pom.xml）

```xml
spring-cloud-starter-gateway          # Spring Cloud Gateway 本体
spring-cloud-starter-loadbalancer     # 配合 lb:// 做负载均衡
spring-cloud-starter-alibaba-nacos-discovery  # 从 Nacos 发现微服务地址
spring-cloud-starter-alibaba-nacos-config     # 可选：从 Nacos 拉配置
spring-cloud-starter-bootstrap        # 加载 bootstrap.yml
talent-common                         # 仅为了用 JwtUtil 解析 Token
```

### 重要约束（注释里写得很清楚）

1. **不能引入 `spring-boot-starter-web`**  
   Gateway 基于 **WebFlux（响应式）**，和传统 Servlet/Tomcat 不兼容。

2. **不能依赖 `talent-auth` 模块**  
   `talent-auth` 会带入 `spring-boot-starter-web`，会导致网关启动失败。  
   所以 JWT 工具类放在 `talent-common` 里，网关和业务服务共用同一套密钥解析逻辑。

---

## 四、配置文件分别干什么？

### 4.1 bootstrap.yml — 先连 Nacos

```yaml
spring:
  application:
    name: talent-gateway          # 注册到 Nacos 的服务名
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: talent-ai-dev    # 必须与 Nacos 里建的命名空间 ID 一致
```

作用：

- 网关启动后把自己注册到 Nacos（服务名 `talent-gateway`）
- 路由里的 `lb://talent-auth` 会去 Nacos 查 `talent-auth` 的真实 IP:端口

### 4.2 application.yml — 端口、路由、CORS

#### 端口

```yaml
server:
  port: 8080
```

#### 路由表（核心）

| 路由 id | 路径匹配 | 转发到（Nacos 服务名） |
|---------|----------|------------------------|
| auth_route | `/api/auth/**` | `lb://talent-auth` |
| job_route | `/api/job/**`、`/api/delivery/**` | `lb://talent-job` |
| resume_route | `/api/resume/**` | `lb://talent-resume` |
| ai_route | `/api/ai/**` | `lb://talent-ai-agent` |
| ai_internal_route | `/internal/ai/**` | `lb://talent-ai-agent` |
| interview_route | `/api/interview/**` | `lb://talent-interview` |
| interview_internal_route | `/internal/interview/**` | `lb://talent-interview` |

`lb://` = **LoadBalancer**，从 Nacos 取实例列表并选一个转发。

示例：前端请求 `POST http://localhost:8080/api/auth/login`  
→ 匹配 `auth_route`  
→ 转发到 Nacos 里注册的某个 `talent-auth` 实例（如 `127.0.0.1:8081`）

#### CORS（跨域）

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOriginPatterns: "*"
      allowedMethods: "*"
      allowedHeaders: "*"
      allowCredentials: true
```

允许前端开发服务器（5173）带 Cookie/Token 访问 8080，避免浏览器拦截跨域请求。

#### discovery.locator

```yaml
discovery:
  locator:
    enabled: true
```

开启后还可按 Nacos 服务名自动生成额外路由；当前项目主要依赖上面手写 `routes`，这项属于扩展能力。

---

## 五、启动类 GatewayApplication.java

```java
@SpringBootApplication
@EnableDiscoveryClient   // 开启 Nacos 服务注册与发现
public class GatewayApplication { ... }
```

没有别的注解：网关不需要 `@MapperScan`、不需要 `@FeignClient`，启动即可监听 8080 并注册到 Nacos。

---

## 六、核心代码：AuthGlobalFilter

文件：`filter/AuthGlobalFilter.java`

实现两个接口：

| 接口 | 作用 |
|------|------|
| `GlobalFilter` | 对所有经过网关的请求生效 |
| `Ordered` | 控制过滤器顺序，`getOrder() = -1` 表示**尽可能早**执行（在转发前） |

### 6.1 处理流程（建议按这个顺序理解）

```
请求进入网关
    │
    ▼
路径在白名单？ ──是──► 直接放行，转发到下游
    │否
    ▼
请求头有 Authorization: Bearer xxx ？ ──否──► 返回 401
    │是
    ▼
JwtUtil.parseToken(token) 合法？ ──否──► 返回 401
    │是
    ▼
从 Token 取出 userId、role
写入请求头 X-User-Id、X-User-Role
    │
    ▼
转发到下游微服务（下游可直接读这两个 Header）
```

### 6.2 白名单 WHITE_LIST

```java
"/api/auth/login",
"/api/auth/register",
"/internal/ai/**",
"/internal/interview/**",
"/api/interview/internal/**"
```

| 路径 | 为何放行 |
|------|----------|
| 登录 / 注册 | 还没有 Token，必须免鉴权 |
| `/internal/**` | 微服务之间 Feign 内部调用，不走用户 Token（生产环境应对内网/IP 做额外限制） |

**不在白名单的接口**（如 `/api/job/**`、`/api/resume/**`）必须带 Token。

### 6.3 Token 怎么取？

规范写法（OAuth2 风格）：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

代码去掉前缀 `"Bearer "`（7 个字符），剩下才是 JWT 字符串。  
前端 `request.js` 里正是这样设置的：

```js
config.headers['Authorization'] = 'Bearer ' + token
```

### 6.4 校验通过后做了什么？

```java
ServerHttpRequest mutatedRequest = request.mutate()
    .header("X-User-Id", userId)
    .header("X-User-Role", role != null ? role : "")
    .build();
```

网关**不会**把原始 Token 再解析一遍传给下游的逻辑层，而是**解一次**，把结果放进自定义 Header：

| Header | 来源 | 下游用法示例 |
|--------|------|--------------|
| `X-User-Id` | JWT 的 `subject`（用户 ID） | `@RequestHeader("X-User-Id") Long userId` |
| `X-User-Role` | JWT 的 `claim("role")` | `@RequestHeader("X-User-Role") String role` |

例如 `JobDeliveryController` 投递简历时：

```java
public Map<String, Object> submit(
    @RequestHeader(value = "X-User-Id", required = false) Long userId,
    @RequestHeader(value = "X-User-Role", required = false) String userRole,
    ...
)
```

业务服务**信任网关传入的 Header**（在微服务架构里，需保证外网不能直接绕过网关访问 8081～8085）。

### 6.5 401 响应

当前实现较简：只设置 HTTP 状态码 401，**没有 JSON  body**。  
前端在 `request.js` 里根据 `code === 401` 或 HTTP 状态处理登出。

### 6.6 JwtUtil 在哪？

在 `talent-common` 模块，与 `talent-auth` 登录签发 Token 使用**同一密钥**：

- 登录：`AuthController` → `JwtUtil.generateToken(userId, role)`
- 网关：`AuthGlobalFilter` → `JwtUtil.parseToken(token)`

两边密钥不一致会导致「能登录但网关永远 401」。

---

## 七、完整请求示例：候选人投递简历

```
1. 用户在前端点击投递
   POST /api/delivery/submit
   Header: Authorization: Bearer <token>

2. Vite 代理 → http://localhost:8080/api/delivery/submit

3. talent-gateway
   - AuthGlobalFilter：非白名单 → 校验 Token → 注入 X-User-Id、X-User-Role
   - 路由 job_route：Path 匹配 /api/delivery/** → lb://talent-job

4. talent-job:8082 收到请求
   - Controller 从 Header 读取 userId、userRole
   - 执行业务，必要时 Feign 调 talent-resume、talent-ai-agent

5. 响应沿原路返回 → 网关 → 前端
```

登录流程则走白名单，不校验 Token：

```
POST /api/auth/login → auth_route → talent-auth:8081 → 返回 token
```

---

## 八、与其他组件的关系

```
                    ┌─────────────┐
                    │    Nacos    │
                    │  8848       │
                    └──────┬──────┘
           注册 / 发现      │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
┌───▼───┐  ┌────────┐  ┌────▼────┐  ┌──────────┐  ...
│gateway│  │ auth   │  │  job    │  │ resume   │
│ 8080  │  │ 8081   │  │ 8082    │  │ 8083     │
└───┬───┘  └────────┘  └─────────┘  └──────────┘
    │
    │ 仅对外暴露 8080
    ▼
  前端 / 外部调用
```

| 组件 | 关系 |
|------|------|
| **Nacos** | 网关通过服务名发现下游地址；网关自身也注册为一个服务 |
| **talent-auth** | 签发 Token；网关不负责登录，只负责校验 |
| **talent-common** | 提供 `JwtUtil`，保证签发与校验一致 |
| **各业务服务** | 只处理业务，从 `X-User-Id` / `X-User-Role` 识别当前用户 |
| **前端** | 只认 8080（或 Vite 代理的 `/api`） |

---

## 九、本地调试要点

1. **启动顺序**：建议先 Nacos，再各业务服务，**最后或同时**启动 gateway（网关转发时下游必须已注册）。
2. **命名空间**：Nacos 中必须有 `talent-ai-dev`，否则 `lb://talent-auth` 找不到实例 → 503。
3. **不要直连业务端口测「完整链路」**：  
   - 直连 `8082` 不会经过网关，没有 `X-User-Id` Header，部分接口会报未登录。  
   - 联调 API 应走 `8080`。
4. **internal 接口**：`/internal/ai/**` 在网关白名单，但仍是经网关转发到 ai-agent；Feign 服务间调用也可直连 Nacos 服务名（视 Feign 配置而定）。

---

## 十、常见问题

### Q1：网关和业务服务有什么区别？

| | Gateway | 业务服务（auth/job/...） |
|--|---------|---------------------------|
| 框架 | WebFlux（响应式） | Spring MVC（Servlet） |
| 端口 | 8080 | 8081～8085 |
| 职责 | 路由、鉴权、CORS | 业务逻辑、数据库 |
| 数据库 | 无 | 有 |

### Q2：为什么登录接口还要经过网关？

统一入口、统一 CORS；登录虽免 Token，但仍走 `auth_route` 转发到 `talent-auth`。

### Q3：新增一个微服务要在网关改什么？

在 `application.yml` 的 `spring.cloud.gateway.routes` 增加一条，例如：

```yaml
- id: analytics_route
  uri: lb://talent-analytics
  predicates:
    - Path=/api/analytics/**
```

并在 Nacos 注册对应服务名 `talent-analytics`。

### Q4：AuthGlobalFilter 能改成每个服务自己验 Token 吗？

可以，但会重复代码、密钥分散、漏验风险高。当前架构是**网关集中鉴权**的常见微服务模式。

### Q5：生产环境还要注意什么？

- 网关对外的 HTTPS、限流、日志脱敏
- 禁止外网直接访问 8081～8085
- JWT 密钥放配置中心，不要硬编码在 `JwtUtil`
- `/internal/**` 白名单应收紧（IP 白名单、mTLS 或内部网关）

---

## 十一、相关文件索引

| 文件 | 说明 |
|------|------|
| `talent-gateway/pom.xml` | 依赖约束（禁 web、禁 auth 模块） |
| `talent-gateway/.../GatewayApplication.java` | 启动 + 启用 Nacos 发现 |
| `talent-gateway/.../AuthGlobalFilter.java` | JWT 鉴权与白名单 |
| `talent-gateway/.../application.yml` | 路由与 CORS |
| `talent-gateway/.../bootstrap.yml` | Nacos 地址与命名空间 |
| `talent-common/.../JwtUtil.java` | Token 签发与解析 |
| `talent-ai-front/vite.config.ts` | 前端代理到 8080 |
| `talent-ai-front/src/utils/request.js` | 自动附加 Bearer Token |
| [talent-auth实现说明.md](./talent-auth实现说明.md) | 登录签发 Token、用户与档案（与网关配合） |
| [talent-job实现说明.md](./talent-job实现说明.md) | 岗位、投递、招聘阶段编排 |
| [talent-resume实现说明.md](./talent-resume实现说明.md) | 简历附件、MinIO、HR 初筛 |
| [talent-ai-agent实现说明.md](./talent-ai-agent实现说明.md) | LLM 解析、人岗匹配、DashScope |

---

## 十二、一句话总结

**talent-gateway = 面向前端的唯一 API 大门**：根据 URL 把请求分给对应微服务，在进门时检查 JWT，并把「当前是谁（userId/role）」通过 Header 告诉后面的服务，业务服务专注写业务即可。
