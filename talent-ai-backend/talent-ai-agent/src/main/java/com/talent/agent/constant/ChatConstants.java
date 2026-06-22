package com.talent.agent.constant;

public final class ChatConstants {

    public static final int ROLE_USER = 1;
    public static final int ROLE_ASSISTANT = 2;

    /** 带入 LLM 的最近消息条数（不含本次用户输入） */
    public static final int MAX_HISTORY_MESSAGES = 10;

    public static final int SESSION_TITLE_MAX_LEN = 30;

    private ChatConstants() {
    }
}
