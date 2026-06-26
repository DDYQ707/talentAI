package com.talent.admin.service;

import com.talent.admin.dto.dashboard.ServiceHealthVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过 Nacos OpenAPI 探测各微服务健康状态。
 * <p>
 * 直接调用 /nacos/v1/ns/catalog/services 一次性拿到所有服务及其健康实例数，
 * healthyInstanceCount &gt; 0 即视为 UP。
 *
 * @author TalentAI
 */
@Slf4j
@Component
public class NacosHealthClient {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 服务名 -> 前端展示名映射 */
    private static final Map<String, String> DISPLAY_NAMES = new HashMap<>();

    static {
        DISPLAY_NAMES.put("talent-gateway", "Gateway");
        DISPLAY_NAMES.put("talent-auth", "Auth-Service");
        DISPLAY_NAMES.put("talent-job", "Job-Service");
        DISPLAY_NAMES.put("talent-talent-pool", "Talent-Pool");
        DISPLAY_NAMES.put("talent-admin", "Admin-Service");
        DISPLAY_NAMES.put("talent-resume", "Resume-Service");
        DISPLAY_NAMES.put("talent-ai-agent", "AI-Agent");
        DISPLAY_NAMES.put("talent-interview", "Interview-Service");
    }

    private final RestTemplate restTemplate;

    @Value("${dashboard.nacos.server-addr:127.0.0.1:8848}")
    private String serverAddr;

    @Value("${dashboard.nacos.namespace:talent-ai-dev}")
    private String namespace;

    /** 预设的全部服务名，用于补全未注册的服务为 DOWN */
    @Value("${dashboard.services:}")
    private List<String> services;

    public NacosHealthClient(RestTemplate dashboardRestTemplate) {
        this.restTemplate = dashboardRestTemplate;
    }

    /**
     * 探测所有服务的健康状态。调用 Nacos catalog/services 接口一次性拿到结果，
     * 再用预设服务名补全未注册的服务为 DOWN。
     * 调用失败时打日志并返回空列表，不影响整个大盘接口。
     */
    @SuppressWarnings("unchecked")
    public List<ServiceHealthVO> probeAll() {
        String now = LocalDateTime.now().format(TS);
        // 保持顺序：先 Nacos 返回的，再补全预设的
        Map<String, ServiceHealthVO> byRawName = new LinkedHashMap<>();

        // 注意：pageNo / pageSize 必填，否则 Nacos 报 primitive 错误
        String url = String.format(
                "http://%s/nacos/v1/ns/catalog/services?namespaceId=%s&pageNo=1&pageSize=100&hasIpCount=false&withInstances=false",
                serverAddr, namespace);

        try {
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            log.info("[Dashboard] Nacos catalog/services 原始响应: {}", body);

            if (body != null && body.get("serviceList") instanceof List<?> serviceList) {
                for (Object o : serviceList) {
                    if (!(o instanceof Map<?, ?> item)) {
                        continue;
                    }
                    String rawName = String.valueOf(item.get("name"));
                    int healthy = toInt(item.get("healthyInstanceCount"));

                    ServiceHealthVO vo = new ServiceHealthVO();
                    vo.setName(DISPLAY_NAMES.getOrDefault(rawName, rawName));
                    vo.setStatus(healthy > 0 ? "UP" : "DOWN");
                    vo.setLatency(0L);
                    vo.setUptime("99.9%");
                    vo.setLastChecked(now);
                    byRawName.put(rawName, vo);
                }
            }
        } catch (Exception e) {
            log.warn("[Dashboard] 调用 Nacos catalog/services 失败: {}", e.getMessage());
            return new ArrayList<>();
        }

        // 补全预设但 Nacos 未返回的服务 -> DOWN
        if (services != null) {
            for (String rawName : services) {
                byRawName.computeIfAbsent(rawName, n -> {
                    ServiceHealthVO vo = new ServiceHealthVO();
                    vo.setName(DISPLAY_NAMES.getOrDefault(n, n));
                    vo.setStatus("DOWN");
                    vo.setLatency(0L);
                    vo.setUptime("99.9%");
                    vo.setLastChecked(now);
                    return vo;
                });
            }
        }

        return new ArrayList<>(byRawName.values());
    }

    private int toInt(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        return 0;
    }
}