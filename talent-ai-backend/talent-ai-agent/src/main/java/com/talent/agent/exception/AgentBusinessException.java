package com.talent.agent.exception;

/**
 * AI Agent 业务异常
 */
public class AgentBusinessException extends RuntimeException {

    public AgentBusinessException(String message) {
        super(message);
    }

    public AgentBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
