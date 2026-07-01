package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.dto.AiParseRetryRequest;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.entity.AiParseTask;
import com.talent.agent.domain.vo.ParseTaskVO;
import com.talent.agent.mapper.AiParseTaskMapper;
import com.talent.agent.service.AiParseService;
import com.talent.agent.service.AiParseTaskProcessor;
import com.talent.agent.service.ParseTaskContextService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiParseServiceImpl implements AiParseService {

    private static final int STATUS_PENDING = 0;

    private final AiParseTaskMapper parseTaskMapper;
    private final AiParseTaskProcessor parseTaskProcessor;
    private final ParseTaskContextService parseTaskContextService;
    private final ResumeFeignClient resumeFeignClient;

    @Override
    @Transactional
    public ParseTaskVO submitParseTask(ParseTaskRequest request) {
        if (request == null || request.getResumeId() == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }
        String parseSource = resolveParseSource(request);
        if ("online".equals(parseSource)) {
            if (request.getAttachmentId() != null) {
                throw new IllegalArgumentException("在线简历解析不应携带 attachmentId");
            }
        } else if (request.getAttachmentId() == null) {
            throw new IllegalArgumentException("附件/合并解析时 attachmentId 不能为空");
        }

        AiParseTask task = new AiParseTask();
        task.setResumeId(request.getResumeId());
        task.setAttachmentId(request.getAttachmentId());
        task.setApplicationId(request.getApplicationId());
        task.setCandidateId(request.getCandidateId());
        task.setModelId(request.getModelId());
        if ("online".equals(parseSource)) {
            task.setFileName("online-resume");
            task.setFileType("online");
        } else if ("merged".equals(parseSource)) {
            task.setFileName(firstNonBlank(request.getFileName(), "merged-resume"));
            task.setFileType("merged");
        } else {
            task.setFileName(request.getFileName());
            task.setFileType(request.getFileType());
        }
        task.setTaskStatus(STATUS_PENDING);
        task.setRawTextLength(0);
        parseTaskMapper.insert(task);

        request.setParseSource(parseSource);
        markResumeParsing(request.getResumeId());
        parseTaskProcessor.processAsync(task.getId(), request);
        return toVO(task);
    }

    @Override
    public ParseTaskVO submitReparse(AiParseRetryRequest request) {
        if (request == null || request.getResumeId() == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }
        ParseTaskRequest parseRequest = parseTaskContextService.buildFromResumeContext(
                request.getResumeId(),
                request.getApplicationId(),
                request.getJobId(),
                request.getCandidateId());
        return submitParseTask(parseRequest);
    }

    private void markResumeParsing(Long resumeId) {
        if (resumeId == null) {
            return;
        }
        try {
            Map<String, Object> res = resumeFeignClient.markParseProcessing(resumeId);
            Object code = res != null ? res.get("code") : null;
            if (code instanceof Number n && n.intValue() != 200) {
                log.warn("简历解析中状态回写失败 resumeId={} msg={}", resumeId, res != null ? res.get("msg") : null);
            }
        } catch (Exception e) {
            log.warn("简历解析中状态回写异常 resumeId={} reason={}", resumeId, e.getMessage());
        }
    }

    private String resolveParseSource(ParseTaskRequest request) {
        if (request.getParseSource() == null) {
            return "attachment";
        }
        String source = request.getParseSource().trim().toLowerCase();
        if ("online".equals(source) || "merged".equals(source)) {
            return source;
        }
        return "attachment";
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    @Override
    public ParseTaskVO getParseTask(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        AiParseTask task = parseTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("解析任务不存在");
        }
        return toVO(task);
    }

    @Override
    public ParseTaskVO getLatestByResumeId(Long resumeId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }
        AiParseTask task = parseTaskMapper.selectOne(
                new LambdaQueryWrapper<AiParseTask>()
                        .eq(AiParseTask::getResumeId, resumeId)
                        .orderByDesc(AiParseTask::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        return task == null ? null : toVO(task);
    }

    private ParseTaskVO toVO(AiParseTask task) {
        ParseTaskVO vo = new ParseTaskVO();
        vo.setTaskId(task.getId());
        vo.setResumeId(task.getResumeId());
        vo.setAttachmentId(task.getAttachmentId());
        vo.setApplicationId(task.getApplicationId());
        vo.setCandidateId(task.getCandidateId());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setRawTextLength(task.getRawTextLength());
        vo.setFileName(task.getFileName());
        vo.setFileType(task.getFileType());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        return vo;
    }
}
