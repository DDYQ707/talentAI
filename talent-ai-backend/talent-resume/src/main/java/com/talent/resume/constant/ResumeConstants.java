package com.talent.resume.constant;

public final class ResumeConstants {

    private ResumeConstants() {
    }

    public static final String ROLE_CANDIDATE = "CANDIDATE";
    public static final String ROLE_HR = "HR";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 预签名预览链接有效期（小时） */
    public static final int PREVIEW_EXPIRE_HOURS = 2;

    public static final int UPLOAD_STATUS_SUCCESS = 1;

    /** 筛选状态：待初筛 */
    public static final int SCREEN_PENDING = 1;

    /** 筛选状态：面试中 */
    public static final int SCREEN_INTERVIEWING = 2;

    /** 筛选状态：已录用 */
    public static final int SCREEN_HIRED = 3;

    /** 筛选状态：已淘汰 */
    public static final int SCREEN_REJECTED = 4;

    public static boolean isValidScreenStatus(Integer status) {
        return status != null && status >= SCREEN_PENDING && status <= SCREEN_REJECTED;
    }
}
