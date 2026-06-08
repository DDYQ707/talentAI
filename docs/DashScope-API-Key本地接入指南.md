# DashScope API Key 本地接入指南

> **适用对象**：需要在本地运行 `talent-ai-agent` 并调用通义千问（Qwen-Max）的队友  
> **适用模块**：`talent-ai-backend/talent-ai-agent`  
> **前置条件**：JDK 17+、Maven 编译通过、MySQL / Nacos 按项目文档已启动（Nacos 配置数为 0 也正常）

---

## 1. 你要做什么

本项目**不会**把 API Key 写进代码或 Git。每个人在本地用自己的百炼 Key，通过环境变量 `DASHSCOPE_API_KEY` 注入。

配置入口在 `application.yml`：

```yaml
ai:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model: qwen-max
    timeout-seconds: 60
```

你只需要：**申请 Key → 配到本机 → 启动服务 → 调测试接口**。

---

## 2. 申请自己的 API Key（每人独立）

1. 打开 [阿里云百炼控制台 → API-KEY 管理](https://bailian.console.aliyun.com/#/api-key)
2. 点击 **「+ 创建 API Key」**
3. 填写描述（如：`张三-本地开发`）
4. 创建成功后，点击 Key 右侧 **复制图标**，复制**完整**字符串（一般以 `sk-` 开头）

> **注意**
>
> - 必须使用 **百炼 / 模型服务灵积** 的 API Key，**不能**用阿里云控制台「AccessKey 管理」里的 AccessKey ID / Secret。
> - 不要把 Key 发到群聊、不要提交到 Git、不要写进 `application.yml`。

---

## 3. 本地配置 API Key（三选一）

环境变量必须配置在 **启动 `talent-ai-agent` 的进程** 里。只在测试用的 PowerShell 窗口里设置，对 IDEA 里点的 Run **无效**。

### 方式 A：IntelliJ IDEA（推荐）

1. 右上角运行配置下拉 → **Edit Configurations…**（编辑配置）
2. 左侧选中 **AiAgentApplication**
3. 若看不到「环境变量」一行，点击 **「修改选项(M)」** → 勾选 **「环境变量」**
4. 在 **环境变量(E)** 中填写（把值换成你复制的 Key）：

   ```text
   DASHSCOPE_API_KEY=sk-你的完整密钥
   ```

   也可用右侧 📄 图标打开表格：

   | Name | Value |
   |------|-------|
   | `DASHSCOPE_API_KEY` | `sk-你的完整密钥` |

5. 点击 **应用 → 确定**
6. **Stop 停掉旧进程 → 再 Run 启动**（改环境变量后必须完全重启）

### 方式 B：PowerShell 终端启动服务

在同一终端里**先设变量、再启动**：

```powershell
cd "你的项目路径\talent-ai-system\talent-ai-backend"

# 若 JAVA_HOME 仍指向 JDK 8，请先切到 17 或 21
$env:JAVA_HOME = "C:\Program Files\java\jdk-21\jdk-21.0.8"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

$env:DASHSCOPE_API_KEY = "sk-你的完整密钥"

mvn -pl talent-ai-agent spring-boot:run
```

### 方式 C：Windows 系统环境变量（长期有效）

1. `Win + R` → 输入 `sysdm.cpl` → **高级** → **环境变量**
2. 在「用户变量」中 **新建**：
   - 变量名：`DASHSCOPE_API_KEY`
   - 变量值：`sk-你的完整密钥`
3. 确认保存后，**关闭并重新打开** IDEA / 终端，再启动服务

---

## 4. 启动前自检

### 4.1 确认 JDK

项目要求 **Java 17+**。若 Maven 报「无效的目标发行版: 17」，说明 `JAVA_HOME` 指向了 JDK 8，请改为 JDK 17 或 21。

### 4.2 确认 Nacos（可选）

本地 `bootstrap.yml` 会连 Nacos，但 **Nacos 里配置数为 0 是正常的**——表示没有手动上传 `talent-ai-agent.yaml`，此时 Key 完全来自本机环境变量 + `application.yml`，无需在 Nacos 再配 Key。

### 4.3 看启动日志

服务启动后，在控制台搜索：

```text
DashScope 配置已加载
```

正常示例：

```text
DashScope 配置已加载: model=qwen-max, baseUrl=https://dashscope.aliyuncs.com/compatible-mode/v1, apiKeyLength=35, apiKeyPrefix=sk-abc12...
```

| 日志现象 | 含义 |
|----------|------|
| `apiKeyPrefix=sk-...` 与百炼控制台一致 | Key 已读对 |
| `DashScope API Key 未配置` | 环境变量没传到 Java 进程 |
| `格式异常：应以 sk- 开头` | 可能用了 AccessKey，不是百炼 Key |
| `疑似未正确解析，包含占位符` | 曾把 Key 错误写进 yml，检查是否仍为 `${DASHSCOPE_API_KEY}` |

---

## 5. 验证 Key 是否有效

建议分两步：**先直连阿里云**，再测本项目接口。

### 5.1 第一步：PowerShell 直连 DashScope（不经过 Java）

确认 Key 本身可用（**把 Key 换成你自己的**）：

```powershell
$headers = @{
  Authorization = "Bearer sk-你的完整密钥"
  "Content-Type" = "application/json"
}

$body = @{
  model = "qwen-max"
  messages = @(
    @{ role = "user"; content = "hi" }
  )
} | ConvertTo-Json -Depth 5 -Compress

Invoke-RestMethod -Method Post `
  -Uri "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" `
  -Headers $headers `
  -Body $body
```

- **成功**：返回带 `choices` 的 JSON，且 `model` 为 `qwen-max`
- **`invalid_api_key`**：Key 无效或类型错误，回第 2 节重新申请
- **`Required body invalid`**：PowerShell 把 JSON 传坏了，用上面 `Invoke-RestMethod` 写法，不要用 `-d` 传复杂 JSON 的 curl

### 5.2 第二步：测 talent-ai-agent 测试接口（直连 8084）

确保 `talent-ai-agent` 已启动（默认端口 **8084**）：

```powershell
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8084/api/ai/test/chat" `
  -ContentType "application/json" `
  -Body '{"prompt":"请只返回：AI服务连接成功"}'
```

**期望响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "content": "AI服务连接成功"
  }
}
```

若终端中文乱码，可先执行：

```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

再以 JSON 形式查看：

```powershell
$r = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8084/api/ai/test/chat" `
  -ContentType "application/json" `
  -Body '{"prompt":"请只返回：AI服务连接成功"}'
$r | ConvertTo-Json -Depth 5
```

### 5.3 第三步（可选）：经网关 + JWT 测试

经网关访问 `/api/ai/**` 需要登录 Token（白名单不含该测试接口）。

**Postman**

| 项 | 值 |
|----|-----|
| Method | `POST` |
| URL | `http://127.0.0.1:8080/api/ai/test/chat` |
| Header | `Content-Type: application/json` |
| Header | `Authorization: Bearer <登录后获取的 JWT>` |
| Body | `{"prompt":"请只返回：AI服务连接成功"}` |

**curl（Windows）**

```bash
curl.exe -X POST "http://127.0.0.1:8080/api/ai/test/chat" ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <JWT>" ^
  -d "{\"prompt\":\"请只返回：AI服务连接成功\"}"
```

---

## 6. 常见问题

### Q1：`invalid_api_key` / Incorrect API key provided

| 可能原因 | 处理 |
|----------|------|
| 用了 AccessKey 而不是百炼 API Key | 在百炼控制台重新创建 `sk-` 开头的 Key |
| IDEA 环境变量未填或未重启 | 检查 Run Configuration → Stop → Run |
| 只在 curl 终端设了变量，服务在 IDEA 里启动 | 必须在 **启动服务的进程** 里配置 Key |
| Key 复制不完整或含空格 | 重新从控制台复制 |
| 曾把 Key 写进 `application.yml` 的错误占位符 | 保持 `api-key: ${DASHSCOPE_API_KEY}`，不要写成 `${sk-xxx}` |

### Q2：返回 `DASHSCOPE_API_KEY 未配置`

环境变量没有传入 Java 进程。按第 3 节重新配置并 **完全重启** 服务。

### Q3：PowerShell 直连成功，Java 仍失败

说明 Key 有效，问题在 Java 侧读到的值不对：

1. 看启动日志 `DashScope 配置已加载` 的 `apiKeyPrefix` 是否与你的 Key 一致  
2. 确认 IDEA 环境变量名是 `DASHSCOPE_API_KEY`（不是 `api-key` 或其他）  
3. 确认 `application.yml` 未被改成硬编码 Key

### Q4：Nacos 命名空间配置数一直是 0

正常。本地 Key 不依赖 Nacos 配置文件；0 表示没有在 Nacos 手动建 `talent-ai-agent.yaml`。

### Q5：Maven 编译失败「无效的目标发行版: 17」

将 `JAVA_HOME` 指向 JDK 17 或 21，并重启终端 / IDEA。

---

## 7. 安全规范（全员遵守）

1. **禁止** 将 API Key 写入 `application.yml`、Java 代码或提交 Git  
2. **禁止** 在截图、文档、群聊中暴露完整 Key  
3. 若 Key 曾泄露，立即在百炼控制台 **禁用 / 删除并重建**  
4. 每人使用 **自己的** Key，便于用量与权限隔离  

---

## 8. 相关代码位置（便于排查）

| 内容 | 路径 |
|------|------|
| 配置项 | `talent-ai-agent/src/main/resources/application.yml` |
| 配置类 | `com.talent.agent.config.DashScopeProperties` |
| LLM 封装 | `com.talent.agent.service.LlmChatService` |
| 实现类 | `com.talent.agent.service.impl.LlmChatServiceImpl` |
| 测试接口 | `POST /api/ai/test/chat` → `AiTestChatController` |
| 启动校验日志 | `com.talent.agent.config.DashScopeStartupValidator` |

---

## 9. 验收清单

队友接入完成后，请逐项勾选：

- [ ] 已在百炼控制台创建 **自己的** API Key（`sk-` 开头）
- [ ] 已在 IDEA 或终端配置 `DASHSCOPE_API_KEY`，且 **未** 写入 Git
- [ ] 启动日志出现 `DashScope 配置已加载`，`apiKeyPrefix` 正确
- [ ] PowerShell 直连 DashScope 返回 `choices`
- [ ] `POST http://127.0.0.1:8084/api/ai/test/chat` 返回 `code: 200`
- [ ] `data.content` 含模型回复（测试 prompt 时为「AI服务连接成功」或语义相近内容）

全部通过后，即表示 **Qwen-Max 最小调用验证** 在本地接入成功，可继续后续 Sprint 开发（简历解析等）。
