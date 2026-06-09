package com.talent.interview.util;

import java.util.Map;

public final class FeignResponseHelper {

    private FeignResponseHelper() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> dataMap(Map<String, Object> response) {
        if (response == null) {
            return Map.of();
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    public static int code(Map<String, Object> response) {
        if (response == null) {
            return 500;
        }
        Object code = response.get("code");
        if (code instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(code));
        } catch (NumberFormatException e) {
            return 500;
        }
    }

    public static String msg(Map<String, Object> response, String defaultMsg) {
        if (response == null) {
            return defaultMsg;
        }
        Object msg = response.get("msg");
        return msg != null ? String.valueOf(msg) : defaultMsg;
    }

    public static Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer intVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String strVal(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
