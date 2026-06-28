# Nacos 配置中心 — 导入说明

> 用于演示 **配置热更新**（答辩评分项）。Nacos 未启动或未导入时，服务仍使用本地 `application.yml` 默认值，**不影响启动**。

## 步骤

1. 打开 Nacos 控制台：`http://127.0.0.1:8848/nacos`（默认 nacos/nacos）
2. 确认命名空间 `talent-ai-dev` 已创建（见部署指南第五节）
3. **配置管理 → 配置列表 → 创建配置**：

| 字段 | 值 |
|------|-----|
| Data ID | `talent-auth.yaml` |
| Group | `DEFAULT_GROUP` |
| 配置格式 | YAML |
| 配置内容 | 复制本目录 `talent-auth.yaml` |

4. 可选：同样创建 `talent-analytics.yaml`（Feign 超时演示）

## 热更新验证

1. 启动 `talent-auth`
2. 调用 `POST /api/auth/otp/send?account=13800138099`，响应中应有 `devCode`
3. 在 Nacos 将 `talent.auth.otp.dev-expose` 改为 `false`，发布
4. 再次调用 send，**无需重启**，`devCode` 应消失

## 相关代码

- `talent-auth/.../OtpProperties.java` — `@RefreshScope`
- `talent-auth/src/main/resources/bootstrap.yml` — `import-check.enabled: false`
