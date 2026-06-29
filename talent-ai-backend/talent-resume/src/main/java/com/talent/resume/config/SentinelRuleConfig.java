package com.talent.resume.config;


import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel 熔断降级规则（本地/代码配置）。
 *
 * <p><b>资源名以运行时实际注册为准，不照文档格式硬猜。</b>
 * Feign + Sentinel 整合后，每个 Feign 方法会被注册为一个 Sentinel 资源。
 * 不同 Spring Cloud / Alibaba 版本生成的资源名格式可能不同（可能是
 * {@code GET:http://talent-job/api/...}，也可能是其它形式）。
 *
 * <p><b>查证实际资源名的步骤（务必先做再填规则）：</b>
 * <ol>
 *   <li>先不配规则（或保留本类但 RESOURCES 留空），启动 talent-resume；</li>
 *   <li>通过页面/接口真实触发一次 JobFeignClient 的调用（如打开 HR 简历详情）；</li>
 *   <li>访问 <code>GET http://localhost:8083/actuator/sentinel</code>，在返回 JSON 中查找
 *       "resourcesAndRules" 或调用统计里 talent-job 相关的资源名，复制其准确字符串；</li>
 *   <li>把准确资源名填入下方 {@link #JOB_FEIGN_RESOURCES}，重启生效。</li>
 * </ol>
 *
 * <p><b>熔断规则说明（异常比例策略）：</b>
 * <ul>
 *   <li>统计时长 1s 内请求数 ≥ {@code minRequestAmount} 且异常比例 ≥ 50% → 触发熔断；</li>
 *   <li>熔断时长 {@code timeWindow} 秒，期间所有请求直接走 fallbackFactory；</li>
 *   <li>熔断结束后进入半开状态，放行一个请求探测下游是否恢复。</li>
 * </ul>
 *
 * <p><b>如需接入 Sentinel Dashboard（可选）：</b>
 * <ol>
 *   <li>下载并启动 sentinel-dashboard jar：<br>
 *       {@code java -Dserver.port=8080 -jar sentinel-dashboard-1.8.x.jar}</li>
 *   <li>application.yml 已配 {@code spring.cloud.sentinel.transport.dashboard=127.0.0.1:8080}；</li>
 *   <li>启动 talent-resume 并触发一次调用后，Dashboard 即可看到本服务并在线配置规则。</li>
 *   <li>注意：Dashboard 推送的规则默认存在内存，重启丢失；生产应配置规则持久化到 Nacos
 *       （sentinel-datasource-nacos），本轮不接，仅用本类的代码规则。</li>
 * </ol>
 */
@Slf4j
@Configuration
public class SentinelRuleConfig {

    /**
     * JobFeignClient 各方法在 Sentinel 中实际注册的资源名。
     *
     * <p>✅ 已通过 /actuator/sentinel（Sentinel metrics 日志）查证并填入实际资源名，对应 4 个 Feign 方法：
     * <pre>
     *   "GET:http://talent-job/api/job/internal/application/latest-by-resume",
     *   "GET:http://talent-job/api/job/internal/application/latest-by-candidate",
     *   "POST:http://talent-job/api/job/internal/application/sync-by-screen-status",
     *   "POST:http://talent-job/api/job/internal/application/latest-by-candidates"
     * </pre>
     * 资源名列表为空时本类不加载任何规则（仅依赖 fallback 兜底），不影响服务启动。
     */
    private static final List<String> JOB_FEIGN_RESOURCES = List.of(
            "GET:http://talent-job/api/job/internal/application/latest-by-resume",
            "GET:http://talent-job/api/job/internal/application/latest-by-candidate",
            "POST:http://talent-job/api/job/internal/application/sync-by-screen-status",
            "POST:http://talent-job/api/job/internal/application/latest-by-candidates"
    );

    @PostConstruct
    public void initDegradeRules() {
        if (JOB_FEIGN_RESOURCES.isEmpty()) {
            log.warn("[Sentinel] 尚未配置 JobFeign 熔断资源名（JOB_FEIGN_RESOURCES 为空）。"
                    + "当前仅 fallbackFactory 生效，未启用主动熔断。"
                    + "请用 GET /actuator/sentinel 查证实际资源名后填入 SentinelRuleConfig。");
            return;
        }
        List<DegradeRule> rules = new ArrayList<>();
        for (String resource : JOB_FEIGN_RESOURCES) {
            rules.add(buildExceptionRatioRule(resource));
        }
        DegradeRuleManager.loadRules(rules);
        log.info("[Sentinel] 已加载 {} 条 JobFeign 熔断降级规则: {}", rules.size(), JOB_FEIGN_RESOURCES);
    }

    /**
     * 异常比例熔断规则：1s 内请求数≥5 且异常比例≥50%，熔断 10s。
     */
    private DegradeRule buildExceptionRatioRule(String resource) {
        DegradeRule rule = new DegradeRule();
        rule.setResource(resource);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(0.5d);            // 异常比例阈值 50%
        rule.setStatIntervalMs(1000);   // 统计窗口 1s
        rule.setMinRequestAmount(5);    // 窗口内至少 5 个请求才可能触发
        rule.setTimeWindow(10);         // 熔断持续 10s
        return rule;
    }
}