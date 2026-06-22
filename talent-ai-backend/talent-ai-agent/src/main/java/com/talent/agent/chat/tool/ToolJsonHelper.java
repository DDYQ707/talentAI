package com.talent.agent.chat.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolJsonHelper {

    private final ObjectMapper objectMapper;

    public String toJson(Object value) {
        if (value == null) {
            return "无数据";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    public String fromFeign(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return "无数据";
        }
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() != 200) {
            Object msg = response.get("msg");
            return msg != null ? msg.toString() : "查询失败";
        }
        Object data = response.get("data");
        if (data != null) {
            return toJson(data);
        }
        return toJson(response);
    }
}
