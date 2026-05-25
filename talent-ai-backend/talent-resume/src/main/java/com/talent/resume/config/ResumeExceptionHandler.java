package com.talent.resume.config;

import com.talent.common.api.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ResumeExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    public R<Void> handleThrowable(Throwable e) {
        log.error("resume service error", e);
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return R.fail("服务异常：" + msg);
    }
}
