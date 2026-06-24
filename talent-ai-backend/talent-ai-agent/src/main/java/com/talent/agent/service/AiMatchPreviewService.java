package com.talent.agent.service;

import com.talent.agent.domain.vo.MatchResultVO;
import java.util.List;
import java.util.Map;

public interface AiMatchPreviewService {

    /** 查询预览匹配结果（不触发新任务） */
    MatchResultVO getPreviewMatch(Long candidateId, Long jobId);

    /** 批量查询预览匹配缓存 */
    Map<Long, MatchResultVO> getPreviewBatch(Long candidateId, List<Long> jobIds);

    /** 触发预览匹配（无投递单；若缺解析会先解析再匹配） */
    MatchResultVO triggerPreviewMatch(Long candidateId, Long jobId);
}
