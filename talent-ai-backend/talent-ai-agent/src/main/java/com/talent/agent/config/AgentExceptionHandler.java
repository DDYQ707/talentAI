package com.talent.agent.config;

import com.talent.agent.exception.AgentBusinessException;
import com.talent.common.api.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AgentExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(AgentBusinessException.class)
    public R<Void> handleBusiness(AgentBusinessException e) {
        log.warn("ai-agent business error: {}", e.getMessage());
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    public R<Void> handleThrowable(Throwable e) {
        log.error("ai-agent service error", e);
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return R.fail("服务异常：" + msg);
    }
}
