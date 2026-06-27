package com.talent.agent.util;

/** 人岗匹配度分级（业务规则：≥80 / 60-79 / <60） */
public final class MatchLevelUtil {

    private MatchLevelUtil() {}

    public static String resolveLevel(Integer score) {
        if (score == null) {
            return null;
        }
        if (score >= 80) {
            return "强烈推荐";
        }
        if (score >= 60) {
            return "可考虑";
        }
        return "暂不匹配";
    }
}
