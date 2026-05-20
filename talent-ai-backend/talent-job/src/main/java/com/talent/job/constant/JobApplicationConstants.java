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

    /** 默认渠道：其他（平台自主投递） */
    public static final byte CHANNEL_OTHER = 5;

    public static final String ROLE_CANDIDATE = "CANDIDATE";
}
