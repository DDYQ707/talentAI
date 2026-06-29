# 热更新压测脚本说明

目标：在 **Nacos 配置热更新期间持续对探针接口打流量**，验证配置切换过程中**错误率为 0**（在途请求不被中断、`@RefreshScope` 平滑重建 Bean）。

目标接口：`GET http://localhost:8083/api/resume/test/probe`

本目录提供两套脚本，任选其一：

- `probe_hotreload.lua` —— **wrk** 脚本（推荐，统计应用层非 2xx + 连接错误，输出错误率/延迟）
- `probe_hotreload.ps1` —— **纯 PowerShell** 备用脚本（Windows 无法装 wrk 时用，无需额外依赖）

---

## 方式一：wrk（推荐）

wrk 不支持原生 Windows，请在 **WSL / Git Bash / Linux / macOS** 中运行。

### 安装 wrk

- Ubuntu/WSL：`sudo apt-get install -y wrk`
- macOS：`brew install wrk`

### 运行

```bash
cd docs/load-test
wrk -t4 -c50 -d60s -s probe_hotreload.lua http://localhost:8083/api/resume/test/probe
```

- `-t4`：4 个线程
- `-c50`：50 个并发连接
- `-d60s`：持续 60 秒

### 验证操作

1. 先看一次接口基线值：浏览器/curl 访问探针接口，记录 `value`（如 `INIT`）。
2. 启动上面的 wrk 命令，压测开始（持续 60s）。
3. **压测进行中**，去 Nacos 控制台 namespace `talent-ai-dev` → Data ID `talent-resume.yaml`（Group `DEFAULT_GROUP`）修改并发布：
   ```yaml
   test:
     dynamic-value: HELLO_NACOS
   ```
4. 压测期间再开一个终端 curl 探针接口，观察 `value` 由 `INIT` 切换为 `HELLO_NACOS`（无需重启）。
5. 压测结束，查看脚本 `done()` 输出的 **错误率应为 0%**。

---

## 方式二：PowerShell 备用脚本（Windows 免安装）

```powershell
cd docs\load-test
.\probe_hotreload.ps1 -DurationSeconds 60 -Concurrency 20
```

脚本会在指定时长内持续并发请求探针接口，结束后打印总请求数、成功数、失败数、错误率、以及观测到的 `value` 取值集合（用于确认热更新前后值的切换）。压测中途按方式一第 3 步去 Nacos 改配置发布即可。

---

## 判定标准

| 指标 | 期望 |
|------|------|
| 错误率 | **0%**（无 5xx、无连接中断） |
| value 切换 | 压测期间由旧值平滑切换为新值，**无需重启** |
| 延迟 | 配置发布瞬间无明显抖动/无请求失败 |
