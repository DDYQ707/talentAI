package com.talent.resume.feign;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * JobFeignClient 的降级工厂。
 *
 * <p>当 talent-job 服务不可用、超时或被 Sentinel 熔断时，会进入这里返回的降级实现，
 * 而不是把异常直接抛给调用方导致故障扩散。
 *
 * <p>降级返回值的设计严格对齐 {@code ResumeService} 中各方法的实际取值习惯：
 * <ul>
 *   <li><b>读接口</b>（查最新投递）：返回空 Map / {@code {"items":{}}}，调用方表现为
 *       “查不到投递信息”，简历详情/列表照常展示，只是不显示投递相关字段。</li>
 *   <li><b>写接口</b>（{@code syncApplicationByScreenStatus}）：返回 {@code code=503}。
 *       调用方 {@code ResumeService.syncApplicationScreenStatus} 检测到非 200 会抛
 *       {@code IllegalStateException}，从而触发 {@code @Transactional} 回滚——
 *       宁可让 HR 操作失败重试，也绝不伪造“同步成功”造成 resume 库与 job 库数据不一致。</li>
 * </ul>
 *
 * <p>所有降级方法均打印 WARN 级告警日志（含原始异常），便于排查下游故障。
 */
@Slf4j
@Component
public class JobFeignClientFallbackFactory implements FallbackFactory<JobFeignClient> {

    /** 降级标记字段，便于调用方/排查时识别这是降级数据而非真实数据 */
    private static final String FALLBACK_FLAG = "_fallback";

    @Override
    public JobFeignClient create(Throwable cause) {
        return new JobFeignClient() {

            @Override
            public Map<String, Object> getLatestApplicationByResume(Long resumeId) {
                log.warn("[Sentinel降级] JobFeignClient.getLatestApplicationByResume 触发降级, resumeId={}, cause={}",
                        resumeId, rootMsg(cause));
                return emptyFallback();
            }

            @Override
            public Map<String, Object> getLatestApplicationByCandidate(Long candidateId) {
                log.warn("[Sentinel降级] JobFeignClient.getLatestApplicationByCandidate 触发降级, candidateId={}, cause={}",
                        candidateId, rootMsg(cause));
                return emptyFallback();
            }

            @Override
            public Map<String, Object> syncApplicationByScreenStatus(Map<String, Object> body) {
                // 写操作：返回非 200，让调用方按既有逻辑抛异常并回滚事务，保证跨服务数据一致性。
                log.warn("[Sentinel降级] JobFeignClient.syncApplicationByScreenStatus 触发降级, "
                                + "投递状态同步未送达 talent-job, body={}, cause={}",
                        body, rootMsg(cause));
                Map<String, Object> res = new HashMap<>();
                res.put("code", 503);
                res.put("msg", "投递状态同步服务暂不可用，请稍后重试");
                res.put(FALLBACK_FLAG, true);
                return res;
            }

            @Override
            public Map<String, Object> getLatestApplicationsByCandidates(Map<String, Object> body) {
                log.warn("[Sentinel降级] JobFeignClient.getLatestApplicationsByCandidates 触发降级, body={}, cause={}",
                        body, rootMsg(cause));
                // 调用方要求 res.containsKey("items") 且 items 为 Map，故返回 {"items":{}}，
                // 遍历空集合后结果为空，与“查不到投递信息”行为完全一致。
                Map<String, Object> res = new HashMap<>();
                res.put("items", new HashMap<>());
                res.put(FALLBACK_FLAG, true);
                return res;
            }
        };
    }

    /**
     * 读接口（单条最新投递）通用降级值。
     *
     * <p>返回真正的空 Map（不放 _fallback 标记），以严格对齐 ResumeService.applyApplicationBrief
     * 中 {@code if (app == null || app.isEmpty()) return;} 的语义——降级即等价于“查不到投递”，
     * 调用方直接 return，不读取任何字段。降级事实已通过上面的 WARN 日志记录，无需再塞标记字段。
     */
    private static Map<String, Object> emptyFallback() {
        return new HashMap<>();
    }

    private static String rootMsg(Throwable cause) {
        return cause == null ? "unknown" : cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}