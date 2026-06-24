package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.entity.AiResumeParseResult;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.domain.vo.ParseTaskVO;
import com.talent.agent.mapper.AiResumeParseResultMapper;
import com.talent.agent.service.AiMatchPreviewService;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.AiParseService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMatchPreviewServiceImpl implements AiMatchPreviewService {

    private static final int MATCH_STATUS_PENDING = 0;
    private static final int MATCH_STATUS_PROCESSING = 1;
    private static final int MATCH_STATUS_SUCCESS = 2;
    private static final int PARSE_STATUS_PENDING = 0;
    private static final int PARSE_STATUS_PROCESSING = 1;

    private final ResumeFeignClient resumeFeignClient;
    private final AiMatchService aiMatchService;
    private final AiParseService aiParseService;
    private final AiResumeParseResultMapper parseResultMapper;

    @Override
    public MatchResultVO getPreviewMatch(Long candidateId, Long jobId) {
        validateCandidateJob(candidateId, jobId);
        Long resumeId = resolvePrimaryResumeId(candidateId);
        if (resumeId == null) {
            return null;
        }
        return aiMatchService.getLatestByResumeAndJob(resumeId, jobId);
    }

    @Override
    public Map<Long, MatchResultVO> getPreviewBatch(Long candidateId, List<Long> jobIds) {
        Map<Long, MatchResultVO> result = new HashMap<>();
        if (candidateId == null || jobIds == null || jobIds.isEmpty()) {
            return result;
        }
        Long resumeId = resolvePrimaryResumeId(candidateId);
        if (resumeId == null) {
            return result;
        }
        for (Long jobId : jobIds) {
            if (jobId == null || jobId <= 0) {
                continue;
            }
            MatchResultVO vo = aiMatchService.getLatestByResumeAndJob(resumeId, jobId);
            if (vo != null) {
                result.put(jobId, vo);
            }
        }
        return result;
    }

    @Override
    public MatchResultVO triggerPreviewMatch(Long candidateId, Long jobId) {
        validateCandidateJob(candidateId, jobId);
        Long resumeId = resolvePrimaryResumeId(candidateId);
        if (resumeId == null) {
            throw new IllegalArgumentException("请先完善在线简历后再查看匹配度");
        }

        MatchResultVO existing = aiMatchService.getLatestByResumeAndJob(resumeId, jobId);
        if (existing != null) {
            Integer status = existing.getMatchStatus();
            if (status != null
                    && (status == MATCH_STATUS_PENDING
                            || status == MATCH_STATUS_PROCESSING
                            || (status == MATCH_STATUS_SUCCESS
                                    && existing.getMatchScore() != null
                                    && existing.getMatchScore() > 0))) {
                return existing;
            }
        }

        if (!hasParseResult(resumeId)) {
            ParseTaskVO latestParse = aiParseService.getLatestByResumeId(resumeId);
            if (latestParse != null) {
                Integer parseStatus = latestParse.getTaskStatus();
                if (parseStatus != null
                        && (parseStatus == PARSE_STATUS_PENDING || parseStatus == PARSE_STATUS_PROCESSING)) {
                    return pendingMatchVo(resumeId, jobId);
                }
            }
            triggerPreviewParse(candidateId, resumeId, jobId);
            return pendingMatchVo(resumeId, jobId);
        }

        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setApplicationId(null);
        matchRequest.setJobId(jobId);
        matchRequest.setResumeId(resumeId);
        return aiMatchService.submitMatch(matchRequest);
    }

    private void validateCandidateJob(Long candidateId, Long jobId) {
        if (candidateId == null) {
            throw new IllegalArgumentException("未登录或用户信息缺失");
        }
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("jobId 不能为空");
        }
    }

    private Long resolvePrimaryResumeId(Long candidateId) {
        Map<String, Object> res = resumeFeignClient.getPrimaryByCandidate(candidateId);
        if (res == null) {
            return null;
        }
        Object code = res.get("code");
        if (!(code instanceof Number codeNum) || codeNum.intValue() != 200) {
            return null;
        }
        Object data = res.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return null;
        }
        Object resumeId = dataMap.get("resumeId");
        if (resumeId instanceof Number num) {
            return num.longValue();
        }
        return null;
    }

    private boolean hasParseResult(Long resumeId) {
        AiResumeParseResult latest = parseResultMapper.selectOne(
                new LambdaQueryWrapper<AiResumeParseResult>()
                        .eq(AiResumeParseResult::getResumeId, resumeId)
                        .orderByDesc(AiResumeParseResult::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        return latest != null && StringUtils.hasText(latest.getParsedJson());
    }

    private void triggerPreviewParse(Long candidateId, Long resumeId, Long jobId) {
        Map<String, Object> ctxRes = resumeFeignClient.getAiParseContext(resumeId);
        if (ctxRes == null) {
            throw new IllegalArgumentException("无法获取简历解析上下文");
        }
        Object code = ctxRes.get("code");
        if (!(code instanceof Number codeNum) || codeNum.intValue() != 200) {
            String msg = ctxRes.get("msg") != null ? String.valueOf(ctxRes.get("msg")) : "简历信息不足";
            throw new IllegalArgumentException(msg);
        }
        Object data = ctxRes.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalArgumentException("请先完善在线简历或上传附件简历");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) dataMap;
        String resumeType = stringVal(ctx.get("resumeType"));
        Long targetResumeId = longVal(ctx.get("id"));
        if (targetResumeId == null) {
            targetResumeId = resumeId;
        }

        ParseTaskRequest request = new ParseTaskRequest();
        request.setResumeId(targetResumeId);
        request.setCandidateId(candidateId);
        request.setJobId(jobId);

        if ("attachment".equalsIgnoreCase(resumeType)) {
            Long attachmentId = longVal(ctx.get("attachmentId"));
            if (attachmentId == null) {
                throw new IllegalArgumentException("附件简历信息缺失，无法解析");
            }
            request.setParseSource("attachment");
            request.setAttachmentId(attachmentId);
            request.setFileName(stringVal(ctx.get("fileName")));
            request.setFileType(stringVal(ctx.get("fileType")));
        } else if ("merged".equalsIgnoreCase(resumeType)) {
            Long attachmentId = longVal(ctx.get("attachmentId"));
            if (attachmentId == null) {
                throw new IllegalArgumentException("合并解析缺少附件信息");
            }
            request.setParseSource("merged");
            request.setAttachmentId(attachmentId);
            request.setFileName(stringVal(ctx.get("fileName")));
            request.setFileType("merged");
        } else if ("online".equalsIgnoreCase(resumeType)) {
            request.setParseSource("online");
        } else {
            throw new IllegalArgumentException("未知简历类型，无法解析");
        }

        aiParseService.submitParseTask(request);
        log.info("预览匹配已触发简历解析 candidateId={} resumeId={} jobId={}", candidateId, targetResumeId, jobId);
    }

    private MatchResultVO pendingMatchVo(Long resumeId, Long jobId) {
        MatchResultVO vo = new MatchResultVO();
        vo.setResumeId(resumeId);
        vo.setJobId(jobId);
        vo.setMatchScore(0);
        vo.setMatchStatus(MATCH_STATUS_PENDING);
        return vo;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longVal(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        return null;
    }
}
