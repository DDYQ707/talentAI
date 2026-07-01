# JMeter 性能测试说明

## 负载测试（50 → 150 用户）

**目标**：P95 响应时间 < 1s

**推荐接口**（需先在 Postman 登录获取 Token）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/job/list?current=1&size=10` | GET | 岗位列表 |
| `/api/analytics/hr/dashboard` | GET | Feign 聚合 |

**线程组建议**：

1. 基准：50 线程，Ramp-Up 10s，循环 10 次，持续 5min
2. 加压：150 线程，Ramp-Up 30s，循环 10 次，持续 5min

**HTTP Header**：`Authorization: Bearer {{hrToken}}`

**Listener**：聚合报告、察看结果树（抽样）、保存 JTL

## 压力测试（至 500 并发）

阶梯线程组：50 → 100 → 200 → 300 → 500，每阶 2min

记录：

- 错误率拐点
- 任务管理器 CPU/内存
- Sentinel 触发时网关 429

## 异常场景

1. 运行 JMeter 时停止 `talent-job` 进程 → dashboard 应仍 200，指标为 0
2. Nacos 修改 OTP 配置 → send 接口行为变化

结果写入 `docs/测试报告-性能测试.md`（需自行创建并附截图）。
