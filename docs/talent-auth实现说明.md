# talent-auth 实现说明

> 模块路径：`talent-ai-backend/talent-auth`  
> 端口：**8081**  
> 数据库：**talent_auth_db**  
> 定位：用户身份、登录注册、候选人档案、管理员账号管理

相关文档：

- [talent-gateway 实现说明](./talent-gateway实现说明.md)
- [talent-job 实现说明](./talent-job实现说明.md)
- [talent-resume 实现说明](./talent-resume实现说明.md)

---

## 一、它解决什么问题？

在微服务架构里，**「谁可以登录、密码怎么存、候选人档案在哪」** 需要有一个专门的服务负责。`talent-auth` 就是这个 **认证与用户中心**。

| 职责 | 说明 |
|------|------|
| **登录 / 注册** | 校验账号密码，签发 JWT Token |
| **用户与档案** | 维护 `sys_user`、`candidate_profile` 等表 |
| **门户分流** | 用 `userType` 区分候选人 / HR / 面试官 / 管理员 |
| **管理员功能** | 创建 HR、面试官账号 |
| **对内 API** | 供 `talent-job` 等微服务 Feign 查询用户名、档案是否完善 |

它**不负责**：

- 在每次业务请求里验 Token（那是 **talent-gateway** 的 `AuthGlobalFilter`）
- 岗位、简历、投递、AI 等业务（分别在 job / resume / agent 等模块）

```
                    ┌─────────────────┐
  登录/注册          │  talent-gateway │  验 Token、注入 X-User-Id
  /api/auth/login   │     :8080       │  /api/auth/candidate/**
        │           └────────┬────────┘
        │                    │ lb://talent-auth
        ▼                    ▼
┌───────────────────────────────────────┐
│           talent-auth :8081            │
│  签发 JWT · 用户表 · 候选人档案 · 管理账号 │
└───────────────────┬───────────────────┘
                    │ talent_auth_db
                    ▼
              MySQL + Redis(预留)
```

---

## 二、和 gateway 怎么分工？

这是理解 auth 的**关键**：

| 环节 | 谁做 | 在哪 |
|------|------|------|
| 用户输入账号密码 | 前端 | LoginView |
| 校验密码、生成 Token | **talent-auth** | `AuthController.login` → `JwtUtil.generateToken` |
| 后续请求携带 Token | 前端 | `Authorization: Bearer xxx` |
| 校验 Token 是否有效 | **talent-gateway** | `AuthGlobalFilter` → `JwtUtil.parseToken` |
| 告诉业务「当前用户是谁」 | **gateway 写 Header** | `X-User-Id`、`X-User-Role` |
| 业务读当前用户 | 各业务服务 / auth 自己 | `@RequestHeader("X-User-Id")` |

**auth 只签发 Token，不拦截每个 API。**  
项目没有启用完整的 Spring Security 过滤器链（见下文 `SecurityConfig`），对外 API 的「门卫」是网关。

---

## 三、模块目录结构

```
talent-auth/
├── pom.xml
└── src/main/
    ├── java/com/talent/auth/
    │   ├── AuthApplication.java           # 启动类
    │   ├── config/
    │   │   ├── SecurityConfig.java        # 仅 BCrypt 密码编码器
    │   │   └── MybatisPlusConfig.java     # 分页插件
    │   ├── controller/                    # HTTP 入口（见第四节）
    │   ├── service/                       # 业务逻辑
    │   ├── mapper/                        # MyBatis 接口
    │   ├── entity/                        # 表实体
    │   ├── dto/ / vo/                     # 请求体 / 返回视图
    │   └── test/CodeGenerator.java        # 代码生成器（开发用）
    └── resources/
        ├── application.yml                # 端口、MySQL、Redis
        ├── bootstrap.yml                  # Nacos
        └── mapper/*.xml                   # MyBatis SQL
```

比 gateway 复杂得多：典型的 **Controller → Service → Mapper → DB** 分层。

---

## 四、依赖与技术栈（pom.xml）

| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-web` | Spring MVC，提供 REST API（Servlet/Tomcat） |
| `spring-security-crypto` | **仅** `BCryptPasswordEncoder`，不是完整 Spring Security 网关 |
| `mybatis-plus-spring-boot3-starter` | ORM、逻辑删除、分页 |
| `spring-boot-starter-data-redis` | 已配置，**当前代码几乎未使用**（预留给验证码等） |
| `nacos-discovery / nacos-config` | 注册为 `talent-auth`，供 gateway / Feign 发现 |
| `talent-common` | 共用 `JwtUtil`、`R` 统一响应等 |

与 gateway 的对比：auth 是 **传统阻塞式 Web**，所以可以（也必须）用 `spring-boot-starter-web`；gateway 则不能用。

---

## 五、数据库：talent_auth_db

脚本：`docs/sql/talent_auth_schema.sql`

| 表 | 作用 |
|----|------|
| `sys_user` | 所有登录用户（候选人、HR、面试官、管理员） |
| `candidate_profile` | 候选人扩展档案（姓名、城市、学历、完整度等） |
| `sys_department` | 部门 |
| `sys_role` / `sys_permission` / `sys_user_role` / `sys_role_permission` | RBAC 权限模型（表已建，业务逐步完善） |
| `auth_verification_code` | 短信/邮箱验证码（Service 目前为空壳） |
| `sys_notification` | 系统通知待办 |

### sys_user 核心字段

| 字段 | 含义 |
|------|------|
| `username` / `phone` / `email` | 登录标识，登录时三者任一匹配即可 |
| `password_hash` | BCrypt 加密后的密码，**从不存明文** |
| `user_type` | **1** 候选人 **2** HR **3** 面试官 **4** 管理员 |
| `status` | **1** 正常 **0** 禁用 |
| `is_deleted` | 逻辑删除（MyBatis-Plus `@TableLogic`） |

### userType 与 JWT role 的映射

登录时在 `AuthController.getRoleString` 中转换，写入 Token 的 `role` claim：

| user_type | JWT role（X-User-Role） | 前端路由倾向 |
|-----------|-------------------------|--------------|
| 1 | `CANDIDATE` | `/candidate` |
| 2 | `HR` | `/hr` |
| 3 | `INTERVIEWER` | `/interviewer` |
| 4 | `ADMIN` | `/admin` |

---

## 六、对外 API 一览（走网关 /api/auth/**）

网关路由：`Path=/api/auth/**` → `lb://talent-auth`

### 6.1 AuthController — `/api/auth`

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/login` | 白名单，无 Token | 密码登录，返回 `token` + `userInfo` |
| POST | `/register` | 白名单 | 候选人自助注册（固定 userType=1） |
| GET | `/getUserName?id=` | 需 Token | 按用户 ID 查显示名（Feign 也会调） |
| GET | `/internal/candidateBrief?userId=` | 需 Token* | 内部档案摘要 |
| GET/POST/PUT/DELETE | `/admin/accounts` | 需 Token + ADMIN | HR/面试官账号 CRUD |

\* `/internal/` 在网关白名单；生产环境应收紧。

### 6.2 CandidateProfileController — `/api/auth/candidate/profile`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/my` | 当前候选人读自己的档案 |
| PUT | `/my` | 保存档案（姓名、手机、城市、学历等） |
| GET | `/complete` | 档案是否完善（投递前 job 服务会 Feign 调用） |
| GET | `/brief?userId=` | 档案摘要（内部） |

当前用户 ID 来自网关注入的 **`X-User-Id`**，不是前端传的 body。

### 6.3 HrCandidateController — `/api/auth/hr`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/candidate-brief?userId=` | HR/管理员查看某候选人档案摘要 |

校验 `X-User-Role` 是否为 `HR` 或 `ADMIN`。

### 6.4 前端实际调用的接口

| 前端文件 | 调用的 auth API |
|----------|-----------------|
| `api/auth.ts` | `/login`、`/register` |
| `api/candidateProfile.ts` | `/candidate/profile/my`、`/complete` |
| `api/adminAccount.ts` | `/admin/accounts` |
| `api/hrCandidate.ts` | `/hr/candidate-brief` |

---

## 七、核心流程讲解

### 7.1 登录流程

```
1. POST /api/auth/login
   Body: username=admin&password=123456  (form-urlencoded)

2. AuthController.login
   ├─ 按 username / phone / email 查 sys_user
   ├─ passwordEncoder.matches(明文, password_hash)   // BCrypt
   ├─ 检查 status == 1
   └─ JwtUtil.generateToken(userId, role)            // 与 gateway 共用密钥

3. 返回 JSON
   {
     "code": 200,
     "token": "eyJ...",
     "userInfo": { "userId", "nickname", "userType" }
   }

4. 前端存入 localStorage.talent_token，后续请求带 Bearer
```

相关代码：`AuthController.java` 第 60～110 行。

### 7.2 注册流程

```
POST /api/auth/register
  account=手机号或邮箱 & password=至少6位

→ 校验格式、是否已注册
→ 新建 sys_user（userType=1, BCrypt 密码）
→ 同事务新建 candidate_profile（空档案）
→ 返回 userInfo（不自动登录，前端一般再调 login）
```

### 7.3 候选人完善档案

```
PUT /api/auth/candidate/profile/my
Header: X-User-Id（网关从 Token 注入）

→ CandidateMyProfileService.saveMyProfile
   ├─ 校验必须是 userType=1
   ├─ 更新 sys_user（phone、email、nickname）
   ├─ 更新 candidate_profile
   └─ evaluateCompleteness 计算完整度 0~100

「是否可投递」看 complete 接口：
  必填：真实姓名、手机号、城市、最高学历
  选填加分：当前职位 currentTitle
```

`talent-job` 投递前通过 Feign 调 `/api/auth/candidate/profile/complete` 检查档案是否完善。

### 7.4 管理员创建 HR / 面试官

```
POST /api/auth/admin/accounts
Header: X-User-Role=ADMIN

→ AdminAccountService.create
   ├─ isAdmin(role) 校验
   ├─ userType 只能是 2(HR) 或 3(面试官)
   └─ BCrypt 存密码
```

权限判断在 **Service 层读 Header**，不是 Spring Security 注解。依赖网关已验 Token 且 Header 不可被外网伪造（需内网隔离业务端口）。

---

## 八、SecurityConfig：只用了 BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

注意：

- **没有** `SecurityFilterChain`、**没有** `@EnableWebSecurity`
- 不会在 auth 服务内拦截 `/api/**`
- 密码注册/登录时用 `encode` / `matches`，与 `seed_admin_user.sql` 里的 `$2b$10$...` 哈希配套

---

## 九、与其他微服务的协作（Feign）

### talent-job 调用示例

```java
@FeignClient(name = "talent-auth")
public interface AuthFeignClient {
    @GetMapping("/api/auth/getUserName")
    String getUserName(@RequestParam("id") Long id);

    @GetMapping("/api/auth/candidate/profile/complete")
    Map<String, Object> getProfileCompleteness(@RequestHeader("X-User-Id") Long userId);
}
```

Feign 从 Nacos 发现 `talent-auth`，HTTP 直连 **8081**（不经过 gateway）。  
`getProfileCompleteness` 需要 `X-User-Id`：job 服务在收到网关转发时已有该 Header，Feign 调用时要自行传递（若未传则 auth 返回未登录）。

---

## 十、代码生成器与「脚手架」Controller

以下 Controller 路径**不在** `/api/auth/**` 下，**网关不会转发**，属于 MyBatis-Plus 生成器产物的 CRUD 骨架，供后台开发或直连 8081 调试：

| Controller | 路径前缀 |
|------------|----------|
| SysUserController | `/sysUser` |
| SysRoleController | `/sysRole` |
| SysPermissionController | `/sysPermission` |
| SysDepartmentController | `/sysDepartment` |
| AuthVerificationCodeController | `/authVerificationCode` |
| ... | |

前端正式功能应只使用 **`/api/auth/**`** 下的接口。

---

## 十一、配置说明

### application.yml

```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/talent_auth_db?...
    username: root
    password: dyq!          # 本机 MySQL；Docker MySQL 见部署指南
  data:
    redis:
      host: 127.0.0.1
      port: 6380            # Docker Redis 映射端口
```

### bootstrap.yml

```yaml
spring:
  application:
    name: talent-auth      # Nacos 服务名，Feign / gateway 靠这个名字找
  cloud:
    nacos:
      discovery:
        namespace: talent-ai-dev
```

---

## 十二、启动类 AuthApplication

```java
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.talent.auth.mapper")
public class AuthApplication { ... }
```

- 注册到 Nacos 为 `talent-auth`
- 扫描 `mapper` 包下所有 MyBatis 接口

---

## 十三、本地调试要点

1. **数据库**：先执行 `docs/sql/talent_auth_schema.sql`
2. **种子管理员**：`docs/sql/seed_admin_user.sql` → admin / 123456
3. **联调请走 8080**：例如 `POST http://localhost:8080/api/auth/login`
4. **直连 8081**：可测 login（无需 Token），但带 `@RequestHeader X-User-Id` 的接口会缺 Header
5. **Redis**：不启动 Redis 时，若未来验证码功能接上可能报错；当前主流程不依赖 Redis

---

## 十四、常见问题

### Q1：为什么 auth 不自己验 JWT？

微服务里常见模式：**网关统一鉴权，业务信 Header**。避免每个服务重复解析 Token、密钥分散。auth 的 `/login`、`/register` 在网关白名单，其余 `/api/auth/**` 也要带 Token（除 internal 等特殊路径）。

### Q2：userType 和 sys_role 有什么区别？

- **userType**：门户分流（去候选人端还是 HR 端），登录写进 JWT 的 `role`
- **sys_role / sys_permission**：细粒度 RBAC，表已设计，部分管理功能尚未全部接入

### Q3：注册为什么只能是候选人？

`/register` 固定 `userType=1`。HR、面试官由管理员在 `/admin/accounts` 创建，避免任意人注册成 HR。

### Q4：登录返回的 token 存在哪？

前端 `localStorage` 键名 `talent_token`；`request.js` 自动加 `Authorization: Bearer ...`。

### Q5：JwtUtil 为什么在 common 不在 auth？

gateway 和 auth 都要用同一套密钥：**auth 签发，gateway 校验**。放 common 避免 gateway 依赖 auth 模块（会带入 web 冲突）。

---

## 十五、相关文件索引

| 文件 | 说明 |
|------|------|
| `controller/AuthController.java` | 登录、注册、管理员账号 |
| `controller/CandidateProfileController.java` | 候选人档案 |
| `controller/HrCandidateController.java` | HR 查候选人 |
| `service/CandidateMyProfileService.java` | 档案业务、完整度计算 |
| `service/AdminAccountService.java` | HR/面试官账号管理 |
| `config/SecurityConfig.java` | BCrypt |
| `entity/SysUser.java` / `CandidateProfile.java` | 核心实体 |
| `talent-common/.../JwtUtil.java` | Token 签发 |
| `talent-job/.../AuthFeignClient.java` | 跨服务调用 auth |
| `docs/sql/talent_auth_schema.sql` | 建库建表 |

---

## 十六、一句话总结

**talent-auth = 系统的「用户与身份中心」**：管账号密码和候选人档案，登录时**签发 JWT**；日常 API 的验 Token 在 **gateway** 完成，业务侧通过 **X-User-Id / X-User-Role** 知道当前是谁。
