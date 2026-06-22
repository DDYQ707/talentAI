package com.talent.agent.util;

import org.springframework.util.StringUtils;

public final class ChatAuthSupport {

    public static final String ROLE_HR = "HR";
    public static final String ROLE_ADMIN = "ADMIN";

    private ChatAuthSupport() {
    }

    public static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("未登录或用户信息缺失");
        }
    }

    public static void requireHr(String role) {
        if (!isHrOrAdmin(role)) {
            throw new IllegalArgumentException("仅 HR 或管理员可使用 AI 助手");
        }
    }

    public static boolean isHrOrAdmin(String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return ROLE_HR.equals(r) || ROLE_ADMIN.equals(r);
    }
}
