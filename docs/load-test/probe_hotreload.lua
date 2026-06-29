-- wrk 压测脚本：热更新期间持续打流量，统计非 2xx 错误率
-- 目标接口：GET /api/resume/test/probe
--
-- 运行（在 WSL / Git Bash / Linux / macOS 下，需先安装 wrk）：
--   wrk -t4 -c50 -d60s -s probe_hotreload.lua http://localhost:8083/api/resume/test/probe
--
-- 参数含义：-t4 线程 -c50 并发连接 -d60s 持续 60 秒
-- 压测进行中去 Nacos 改 test.dynamic-value 并发布，结束后看本脚本 done() 输出的 Non-2xx 是否为 0。

local errors = 0
local total  = 0

response = function(status, headers, body)
    total = total + 1
    if status < 200 or status >= 300 then
        errors = errors + 1
    end
end

done = function(summary, latency, requests)
    io.write("\n========== 热更新压测结果 ==========\n")
    io.write(string.format("总请求数            : %d\n", summary.requests))
    io.write(string.format("应用层非2xx响应数    : %d\n", errors))
    io.write(string.format("连接/读写/超时错误    : %d (connect=%d read=%d write=%d timeout=%d)\n",
        summary.errors.connect + summary.errors.read + summary.errors.write + summary.errors.timeout,
        summary.errors.connect, summary.errors.read, summary.errors.write, summary.errors.timeout))
    local errRate = 0
    if summary.requests > 0 then
        errRate = (errors + summary.errors.connect + summary.errors.read
                   + summary.errors.write + summary.errors.timeout) / summary.requests * 100
    end
    io.write(string.format("错误率              : %.4f%%  (期望 0%%)\n", errRate))
    io.write(string.format("平均延迟            : %.2f ms\n", latency.mean / 1000))
    io.write(string.format("P99 延迟            : %.2f ms\n", latency:percentile(99) / 1000))
    io.write("===================================\n")
end
