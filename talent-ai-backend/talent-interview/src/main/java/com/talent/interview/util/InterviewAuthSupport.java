package com.talent.interview.util;

import org.springframework.util.StringUtils;

public final class InterviewAuthSupport {

    public static final String ROLE_HR = "HR";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_INTERVIEWER = "INTERVIEWER";
    public static final String ROLE_CANDIDATE = "CANDIDATE";

    private InterviewAuthSupport() {
    }

    public static void requireHr(String role) {
        if (!isHrOrAdmin(role)) {
            throw new IllegalArgumentException("仅 HR 或管理员可操作");
        }
    }

    public static void requireInterviewer(String role) {
        if (!isInterviewer(role)) {
            throw new IllegalArgumentException("仅面试官可操作");
        }
    }

    public static void requireCandidate(String role) {
        if (!isCandidate(role)) {
            throw new IllegalArgumentException("仅候选人可操作");
        }
    }

    public static boolean isHrOrAdmin(String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return ROLE_HR.equals(r) || ROLE_ADMIN.equals(r);
    }

    public static boolean isInterviewer(String role) {
        return StringUtils.hasText(role) && ROLE_INTERVIEWER.equalsIgnoreCase(role.trim());
    }

    public static boolean isCandidate(String role) {
        return StringUtils.hasText(role) && ROLE_CANDIDATE.equalsIgnoreCase(role.trim());
    }

    public static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("未识别当前用户，请重新登录");
        }
    }
}
