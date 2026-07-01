package com.talent.agent.util;

/**
 * 面试轮次与类型工具（与前端 ROUND_TYPE_OPTIONS 一致，供 AI 面试题生成使用）
 */
public final class InterviewRoundUtil {

    public static final int MAX_INTERVIEW_ROUNDS = 3;

    private static final int ROUND_TYPE_TECH_FIRST = 2;
    private static final int ROUND_TYPE_TECH_SECOND = 3;
    private static final int ROUND_TYPE_HR = 4;
    private static final int ROUND_TYPE_FINAL = 5;

    private InterviewRoundUtil() {}

    public static int normalizeRoundNo(Integer roundNo) {
        if (roundNo == null || roundNo < 1) {
            return 1;
        }
        return Math.min(roundNo, MAX_INTERVIEW_ROUNDS);
    }

    public static String roundTypeLabel(Integer roundType) {
        if (roundType == null) {
            return "面试";
        }
        return switch (roundType) {
            case ROUND_TYPE_TECH_FIRST -> "技术面初面";
            case ROUND_TYPE_TECH_SECOND -> "技术面复试";
            case ROUND_TYPE_HR -> "HR面";
            case ROUND_TYPE_FINAL -> "终面/综合面";
            default -> "面试";
        };
    }

    /** 按 HR 安排的面试类型给出出题侧重点（不按轮次序号臆测类型） */
    public static String roundQuestionGuidance(Integer roundType, Integer roundNo) {
        int round = normalizeRoundNo(roundNo);
        if (roundType == null) {
            return "按通用面试标准出题，兼顾项目经验、岗位契合与情景应对。";
        }
        return switch (roundType) {
            case ROUND_TYPE_TECH_FIRST ->
                    "技术面初面：侧重技术深度、项目经验、关键技能实操；可含 1～2 道情景应对题验证问题解决能力。"
                    + (round > 1 ? " 本轮为第 " + round + " 轮，须在前轮基础上加深考察。" : "");
            case ROUND_TYPE_TECH_SECOND ->
                    "技术面复试：在初面基础上加深技术深度与架构/design 能力；可追问项目难点、性能优化、技术选型理由。"
                    + (round > 1 ? " 本轮为第 " + round + " 轮，避免重复初面基础题。" : "");
            case ROUND_TYPE_HR ->
                    "HR 面：侧重软技能、团队协作、职业规划、文化契合；须含「薪资期望」类题目（期望薪资、入职时间等）。"
                    + (round > 1 ? " 结合前轮评价递进，勿重复纯技术深挖。" : "");
            case ROUND_TYPE_FINAL ->
                    "终面/综合面：综合评估岗位匹配、 leadership/潜力、战略思维；可含情景应对与「薪资期望」题。"
                    + (round > 1 ? " 在前轮结论基础上做终局判断，题目应更高层次。" : "");
            default -> "按通用面试标准出题，兼顾项目经验、岗位契合与情景应对。";
        };
    }
}
