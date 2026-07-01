package com.talent.interview.constant;

/**
 * 面试状态与轮次常量
 */
public final class InterviewConstants {

    private InterviewConstants() {
    }

    /** 待面试 */
    public static final int STATUS_PENDING = 1;

    /** 评价维度：沟通能力 */
    public static final String DIM_COMMUNICATION = "沟通能力";

    /** 评价维度：专业技能 */
    public static final String DIM_PROFESSIONAL = "专业技能";

    /** 评价维度：岗位匹配度 */
    public static final String DIM_JOB_MATCH = "岗位匹配度";

    public static final java.util.List<String> REQUIRED_EVALUATION_DIMENSIONS =
            java.util.List.of(DIM_COMMUNICATION, DIM_PROFESSIONAL, DIM_JOB_MATCH);

    /** 已完成 */
    public static final int STATUS_COMPLETED = 2;

    /** 待安排 */
    public static final int STATUS_TO_SCHEDULE = 3;

    /** 已取消 */
    public static final int STATUS_CANCELLED = 4;

    /** 评价结论：通过 */
    public static final int CONCLUSION_PASS = 1;

    /** 评价结论：待定 */
    public static final int CONCLUSION_HOLD = 2;

    /** 评价结论：不通过 */
    public static final int CONCLUSION_REJECT = 3;

    /** 每份投递最多 3 轮；实际可只安排 1 轮、2 轮或 3 轮，由 HR 灵活决定 */
    public static final int MAX_INTERVIEW_ROUNDS = 3;

    /** 轮次类型：1-AI初筛 2-技术面初面 3-技术面复试 4-HR面 5-终面/综合面 6-交叉面 7-作品评审 */
    public static final int ROUND_TYPE_AI_SCREEN = 1;
    public static final int ROUND_TYPE_TECH_FIRST = 2;
    public static final int ROUND_TYPE_TECH_SECOND = 3;
    public static final int ROUND_TYPE_HR = 4;
    public static final int ROUND_TYPE_FINAL = 5;
    public static final int ROUND_TYPE_CROSS = 6;
    public static final int ROUND_TYPE_PORTFOLIO = 7;

    public static boolean isTechnicalRoundType(Integer roundType) {
        return roundType != null
                && (roundType == ROUND_TYPE_TECH_FIRST || roundType == ROUND_TYPE_TECH_SECOND);
    }

    public static boolean isComprehensiveRoundType(Integer roundType) {
        return roundType != null
                && (roundType == ROUND_TYPE_HR
                        || roundType == ROUND_TYPE_FINAL
                        || roundType == ROUND_TYPE_CROSS);
    }

    public static boolean isValidStatus(Integer status) {
        return status != null
                && (status == STATUS_PENDING
                        || status == STATUS_COMPLETED
                        || status == STATUS_TO_SCHEDULE
                        || status == STATUS_CANCELLED);
    }

    public static boolean isValidConclusion(Integer conclusion) {
        return conclusion != null
                && (conclusion == CONCLUSION_PASS
                        || conclusion == CONCLUSION_HOLD
                        || conclusion == CONCLUSION_REJECT);
    }

    /** 轮次类型：1-AI初筛 2-业务初试 3-业务复试 4-HR面 5-终面 6-交叉面 7-作品评审 */
    public static boolean isValidRoundType(Integer roundType) {
        return roundType != null && roundType >= 1 && roundType <= 7;
    }

    /** 1-视频 2-现场 3-线上评审 */
    public static boolean isValidInterviewMode(Integer mode) {
        return mode != null && mode >= 1 && mode <= 3;
    }

    public static String roundTypeLabel(Integer roundType) {
        if (roundType == null) {
            return "未知";
        }
        return switch (roundType) {
            case ROUND_TYPE_AI_SCREEN -> "AI初筛";
            case ROUND_TYPE_TECH_FIRST -> "技术面初面";
            case ROUND_TYPE_TECH_SECOND -> "技术面复试";
            case ROUND_TYPE_HR -> "HR面";
            case ROUND_TYPE_FINAL -> "终面/综合面";
            case ROUND_TYPE_CROSS -> "交叉面";
            case ROUND_TYPE_PORTFOLIO -> "作品评审";
            default -> "未知";
        };
    }

    public static String interviewModeLabel(Integer mode) {
        if (mode == null) {
            return "未知";
        }
        return switch (mode) {
            case 1 -> "视频面试";
            case 2 -> "现场面试";
            case 3 -> "线上评审";
            default -> "未知";
        };
    }

    public static String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_PENDING -> "待面试";
            case STATUS_COMPLETED -> "面试完成";
            case STATUS_TO_SCHEDULE -> "待安排";
            case STATUS_CANCELLED -> "已取消";
            default -> "未知";
        };
    }

    public static String conclusionLabel(Integer conclusion) {
        if (conclusion == null) {
            return "未知";
        }
        return switch (conclusion) {
            case CONCLUSION_PASS -> "通过";
            case CONCLUSION_HOLD -> "待定";
            case CONCLUSION_REJECT -> "不通过";
            default -> "未知";
        };
    }
}
