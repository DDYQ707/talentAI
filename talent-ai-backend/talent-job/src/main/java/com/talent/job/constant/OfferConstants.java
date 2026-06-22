package com.talent.job.constant;

/**
 * Offer 相关枚举常量（与数据库表结构设计一致）
 */
public final class OfferConstants {

    private OfferConstants() {
    }

    // ==================== Offer 状态 ====================

    /** Offer 状态：待审批 */
    public static final byte OFFER_STATUS_PENDING = 1;

    /** Offer 状态：审批中 */
    public static final byte OFFER_STATUS_APPROVING = 2;

    /** Offer 状态：已通过 */
    public static final byte OFFER_STATUS_APPROVED = 3;

    /** Offer 状态：已拒绝（审批拒绝） */
    public static final byte OFFER_STATUS_REJECTED = 4;

    /** Offer 状态：已发放 */
    public static final byte OFFER_STATUS_ISSUED = 5;

    /** Offer 状态：候选人已接受 */
    public static final byte OFFER_STATUS_ACCEPTED = 6;

    /** Offer 状态：候选人已拒绝 */
    public static final byte OFFER_STATUS_DECLINED = 7;

    /** Offer 状态：已撤回 */
    public static final byte OFFER_STATUS_REVOKED = 8;

    // ==================== 审批节点状态 ====================

    /** 审批节点状态：待审批 */
    public static final byte APPROVAL_STATUS_PENDING = 1;

    /** 审批节点状态：已通过 */
    public static final byte APPROVAL_STATUS_APPROVED = 2;

    /** 审批节点状态：已拒绝 */
    public static final byte APPROVAL_STATUS_REJECTED = 3;

    // ==================== Offer 编号前缀 ====================

    /** Offer 编号前缀 */
    public static final String OFFER_NO_PREFIX = "OFR";

    /**
     * 获取 Offer 状态描述
     */
    public static String offerStatusText(byte status) {
        return switch (status) {
            case OFFER_STATUS_PENDING -> "待审批";
            case OFFER_STATUS_APPROVING -> "审批中";
            case OFFER_STATUS_APPROVED -> "已通过";
            case OFFER_STATUS_REJECTED -> "已拒绝";
            case OFFER_STATUS_ISSUED -> "已发放";
            case OFFER_STATUS_ACCEPTED -> "候选人已接受";
            case OFFER_STATUS_DECLINED -> "候选人已拒绝";
            case OFFER_STATUS_REVOKED -> "已撤回";
            default -> "未知状态";
        };
    }

    /**
     * 获取审批节点状态描述
     */
    public static String approvalStatusText(byte status) {
        return switch (status) {
            case APPROVAL_STATUS_PENDING -> "待审批";
            case APPROVAL_STATUS_APPROVED -> "已通过";
            case APPROVAL_STATUS_REJECTED -> "已拒绝";
            default -> "未知状态";
        };
    }
}
