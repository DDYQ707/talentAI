package com.talent.interview.service;

import com.talent.interview.entity.Interview;

/** 面试流程中的异步联动（筛选状态、AI 画像等），失败不影响主事务 */
public interface InterviewAsyncSideEffectService {

    void afterEvaluationSubmitted(Interview interview, Integer conclusion);

    void revertToPendingScreenAfterCancel(Interview interview);
}
