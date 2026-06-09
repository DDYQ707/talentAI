package com.talent.interview.constant;

/**
 * 面试状态与轮次常量
 */
public final class InterviewConstants {

    private InterviewConstants() {
    }

    /** 待进行 */
    public static final int STATUS_PENDING = 1;

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
            case 1 -> "AI初筛";
            case 2 -> "业务初试";
            case 3 -> "业务复试";
            case 4 -> "HR初面";
            case 5 -> "终面";
            case 6 -> "交叉面";
            case 7 -> "作品评审";
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
            case STATUS_PENDING -> "待进行";
            case STATUS_COMPLETED -> "已完成";
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
