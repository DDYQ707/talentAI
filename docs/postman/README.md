# 接口测试说明（Postman）

> 导入 `TalentAI.postman_collection.json` 到 Postman，设置环境变量 `baseUrl=http://localhost:8080`。

## 覆盖评分要求

- 核心接口 ≥ 6
- 总请求 ≥ 20（含正常/异常变体）
- 验证网关鉴权 + Feign 聚合

## 推荐执行顺序

1. **01 登录 HR** → 自动保存 `hrToken`
2. **02 登录 无Token访问岗位** → 期望 401
3. **03-08** 带 Token 业务接口
4. **09 停 talent-job 后 Dashboard** → 验证降级（需 Sentinel）
5. **10 候选人登录**

## 截图清单（写入测试报告）

- Nacos 服务列表（9 服务 healthy）
- Postman Collection Runner 结果
- 401 / 429 响应体
