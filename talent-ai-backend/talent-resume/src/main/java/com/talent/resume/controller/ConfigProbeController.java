package com.talent.resume.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nacos 配置热更新测试探针。
 *
 * <p>只读、无副作用，不依赖任何业务组件，仅用于验证：
 * 修改 Nacos 中的 {@code test.dynamic-value} 后，<b>无需重启服务</b>，
 * 本接口返回的 value 会自动切换为新值。
 *
 * <p>{@link RefreshScope} 使该 Bean 在收到 Nacos 配置变更事件时被销毁并重建，
 * 重建时重新注入最新的 {@code @Value}，从而实现热更新。
 */
@RefreshScope
@RestController
@RequestMapping("/api/resume/test")
public class ConfigProbeController {

    /** 从 Nacos 读取，未配置时使用本地默认值 INIT */
    @Value("${test.dynamic-value:INIT}")
    private String dynamicValue;

    @GetMapping("/probe")
    public Map<String, Object> probe() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", dynamicValue);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}