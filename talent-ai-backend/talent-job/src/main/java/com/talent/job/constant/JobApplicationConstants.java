package com.talent.job.constant;

/**
 * 投递申请相关枚举常量（与数据库表结构设计一致）
 */
public final class JobApplicationConstants {

    private JobApplicationConstants() {
    }

    /** 岗位状态：招聘中 */
    public static final byte JOB_STATUS_OPEN = 1;

    /** 投递状态：进行中 */
    public static final byte STATUS_IN_PROGRESS = 1;

    /** 招聘阶段：简历投递 */
    public static final byte STAGE_RESUME_SUBMITTED = 1;

    /** 招聘阶段：AI 初筛 */
    public static final byte STAGE_AI_SCREENING = 2;

    /** 招聘阶段：HR 初面 */
    public static final byte STAGE_HR_INTERVIEW = 3;

    /** 招聘阶段：Offer */
    public static final byte STAGE_OFFER = 6;

    /** 投递状态：已录用 */
    public static final byte STATUS_HIRED = 2;

    /** 投递状态：已淘汰 */
    public static final byte STATUS_REJECTED = 3;

    /** 默认渠道：其他（平台自主投递） */
    public static final byte CHANNEL_OTHER = 5;

    public static final String ROLE_CANDIDATE = "CANDIDATE";

    /**
     * resume.screen_status → job_application.current_stage
     * 1-待初筛 2-面试中 3-已录用 4-已淘汰
     */
    public static byte stageForScreenStatus(int screenStatus) {
        return switch (screenStatus) {
            case 2 -> STAGE_HR_INTERVIEW;
            case 3 -> STAGE_OFFER;
            case 4 -> STAGE_AI_SCREENING;
            default -> STAGE_RESUME_SUBMITTED;
        };
    }

    /** resume.screen_status → job_application.status */
    public static byte applicationStatusForScreenStatus(int screenStatus) {
        return switch (screenStatus) {
            case 3 -> STATUS_HIRED;
            case 4 -> STATUS_REJECTED;
            default -> STATUS_IN_PROGRESS;
        };
    }

    public static String actionNoteForScreenStatus(int screenStatus) {
        return switch (screenStatus) {
            case 2 -> "HR 标记为面试中";
            case 3 -> "HR 标记为已录用";
            case 4 -> "HR 标记为已淘汰";
            default -> "HR 标记为待初筛";
        };
    }
}
