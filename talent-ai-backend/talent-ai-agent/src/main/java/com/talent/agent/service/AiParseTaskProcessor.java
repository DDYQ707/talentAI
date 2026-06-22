package com.talent.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.talent.agent.client.JobFeignClient;
import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.dto.ResumeAttachmentInfo;
import com.talent.agent.domain.entity.AiModel;
import com.talent.agent.domain.entity.AiParseTask;
import com.talent.agent.mapper.AiModelMapper;
import com.talent.agent.mapper.AiParseTaskMapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiParseTaskProcessor {

    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_SUCCESS = 2;
    private static final int STATUS_FAILED = 3;

    private static final int MAX_ERROR_LENGTH = 500;
    private static final String DEFAULT_MODEL_CODE = "qwen-max";

    private final AiParseTaskMapper parseTaskMapper;
    private final AiModelMapper modelMapper;
    private final ResumeAttachmentQueryService attachmentQueryService;
    private final MinioDownloadService minioDownloadService;
    private final ResumeTextExtractService textExtractService;
    private final ResumeLlmParseService resumeLlmParseService;
    private final OnlineResumeStructuredParseService onlineResumeStructuredParseService;
    private final ResumeParseMergeService resumeParseMergeService;
    private final AiResumeParseResultService parseResultService;
    private final AiMatchService aiMatchService;
    private final JobFeignClient jobFeignClient;

    @Async
    public void processAsync(Long taskId, ParseTaskRequest request) {
        if (request != null && "online".equalsIgnoreCase(request.getParseSource())) {
            processOnlineAsync(taskId, request);
            return;
        }
        if (request != null && "merged".equalsIgnoreCase(request.getParseSource())) {
            processMergedAsync(taskId, request);
            return;
        }
        markProcessing(taskId);
        int textLength = 0;
        try {
            ResumeAttachmentInfo attachment = attachmentQueryService.fetchByAttachmentId(request.getAttachmentId());
            byte[] fileBytes = minioDownloadService.download(attachment.getBucketName(), attachment.getObjectKey());
            ResumeTextExtractService.ExtractResult extractResult = textExtractService.extract(
                    fileBytes, attachment.getFileName(), attachment.getFileType());
            textLength = extractResult.length();

            log.info(
                    "简历文本抽取成功 taskId={} attachmentId={} resumeId={} fileName={} textLength={}",
                    taskId,
                    request.getAttachmentId(),
                    request.getResumeId(),
                    attachment.getFileName(),
                    textLength);

            LlmParseOutcome parseOutcome = resumeLlmParseService.parse(extractResult.text());
            Long resumeId = firstNonNull(request.getResumeId(), attachment.getResumeId());
            parseResultService.save(taskId, resumeId, parseOutcome, textLength);
            markSuccess(taskId, request, attachment, textLength);

            log.info(
                    "简历结构化解析成功 taskId={} resumeId={} targetPosition={}",
                    taskId,
                    resumeId,
                    parseOutcome.resume().getTargetPosition());
            triggerMatchAfterParse(taskId, request, resumeId);
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            markFailed(taskId, request, reason, textLength);
            log.warn(
                    "简历解析失败 taskId={} attachmentId={} resumeId={} reason={}",
                    taskId,
                    request.getAttachmentId(),
                    request.getResumeId(),
                    truncate(reason));
        }
    }

    /** 方案 C：附件 LLM 解析为主，在线结构化字段补充合并 */
    private void processMergedAsync(Long taskId, ParseTaskRequest request) {
        markProcessing(taskId);
        int textLength = 0;
        try {
            ResumeAttachmentInfo attachment = attachmentQueryService.fetchByAttachmentId(request.getAttachmentId());
            byte[] fileBytes = minioDownloadService.download(attachment.getBucketName(), attachment.getObjectKey());
            ResumeTextExtractService.ExtractResult extractResult = textExtractService.extract(
                    fileBytes, attachment.getFileName(), attachment.getFileType());
            textLength = extractResult.length();

            log.info(
                    "合并解析-附件文本抽取成功 taskId={} attachmentId={} resumeId={} textLength={}",
                    taskId,
                    request.getAttachmentId(),
                    request.getResumeId(),
                    textLength);

            LlmParseOutcome attachmentOutcome = resumeLlmParseService.parse(extractResult.text());
            var onlineSupplement = onlineResumeStructuredParseService.parseSupplementFromResumeId(request.getResumeId());
            LlmParseOutcome mergedOutcome = resumeParseMergeService.merge(attachmentOutcome, onlineSupplement);

            Long resumeId = firstNonNull(request.getResumeId(), attachment.getResumeId());
            parseResultService.save(taskId, resumeId, mergedOutcome, textLength);
            markMergedSuccess(taskId, request, attachment, textLength);

            log.info(
                    "合并简历解析成功 taskId={} resumeId={} targetPosition={}",
                    taskId,
                    resumeId,
                    mergedOutcome.resume().getTargetPosition());
            triggerMatchAfterParse(taskId, request, resumeId);
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            markFailed(taskId, request, reason, textLength);
            log.warn(
                    "合并简历解析失败 taskId={} attachmentId={} resumeId={} reason={}",
                    taskId,
                    request.getAttachmentId(),
                    request.getResumeId(),
                    truncate(reason));
        }
    }

    private void processOnlineAsync(Long taskId, ParseTaskRequest request) {
        markProcessing(taskId);
        int textLength = 0;
        try {
            LlmParseOutcome parseOutcome = onlineResumeStructuredParseService.parseFromResumeId(request.getResumeId());
            textLength = parseOutcome.rawJson() != null ? parseOutcome.rawJson().length() : 0;

            parseResultService.save(taskId, request.getResumeId(), parseOutcome, textLength);
            markOnlineSuccess(taskId, request, textLength);

            log.info(
                    "在线简历结构化解析成功 taskId={} resumeId={} targetPosition={}",
                    taskId,
                    request.getResumeId(),
                    parseOutcome.resume().getTargetPosition());
            triggerMatchAfterParse(taskId, request, request.getResumeId());
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            markFailed(taskId, request, reason, textLength);
            log.warn(
                    "在线简历解析失败 taskId={} resumeId={} reason={}",
                    taskId,
                    request.getResumeId(),
                    truncate(reason));
        }
    }

    private void markOnlineSuccess(Long taskId, ParseTaskRequest request, int textLength) {
        parseTaskMapper.update(
                null,
                new LambdaUpdateWrapper<AiParseTask>()
                        .eq(AiParseTask::getId, taskId)
                        .set(AiParseTask::getTaskStatus, STATUS_SUCCESS)
                        .set(AiParseTask::getResumeId, request.getResumeId())
                        .set(AiParseTask::getAttachmentId, null)
                        .set(AiParseTask::getApplicationId, request.getApplicationId())
                        .set(AiParseTask::getCandidateId, request.getCandidateId())
                        .set(AiParseTask::getModelId, resolveModelId(request.getModelId()))
                        .set(AiParseTask::getRawTextLength, textLength)
                        .set(AiParseTask::getErrorMessage, null)
                        .set(AiParseTask::getFileName, "online-resume")
                        .set(AiParseTask::getFileType, "online")
                        .set(AiParseTask::getFinishedAt, LocalDateTime.now()));
    }

    private void markMergedSuccess(Long taskId, ParseTaskRequest request, ResumeAttachmentInfo attachment, int textLength) {
        parseTaskMapper.update(
                null,
                new LambdaUpdateWrapper<AiParseTask>()
                        .eq(AiParseTask::getId, taskId)
                        .set(AiParseTask::getTaskStatus, STATUS_SUCCESS)
                        .set(AiParseTask::getResumeId, firstNonNull(request.getResumeId(), attachment.getResumeId()))
                        .set(AiParseTask::getAttachmentId, request.getAttachmentId())
                        .set(AiParseTask::getApplicationId, request.getApplicationId())
                        .set(AiParseTask::getCandidateId, request.getCandidateId())
                        .set(AiParseTask::getModelId, resolveModelId(request.getModelId()))
                        .set(AiParseTask::getRawTextLength, textLength)
                        .set(AiParseTask::getErrorMessage, null)
                        .set(AiParseTask::getFileName, firstNonBlank(request.getFileName(), attachment.getFileName()))
                        .set(AiParseTask::getFileType, "merged")
                        .set(AiParseTask::getFinishedAt, LocalDateTime.now()));
    }

    private void markProcessing(Long taskId) {
        AiParseTask update = new AiParseTask();
        update.setId(taskId);
        update.setTaskStatus(STATUS_PROCESSING);
        update.setStartedAt(LocalDateTime.now());
        parseTaskMapper.updateById(update);
    }

    private void markSuccess(Long taskId, ParseTaskRequest request, ResumeAttachmentInfo attachment, int textLength) {
        parseTaskMapper.update(
                null,
                new LambdaUpdateWrapper<AiParseTask>()
                        .eq(AiParseTask::getId, taskId)
                        .set(AiParseTask::getTaskStatus, STATUS_SUCCESS)
                        .set(AiParseTask::getResumeId, firstNonNull(request.getResumeId(), attachment.getResumeId()))
                        .set(AiParseTask::getAttachmentId, request.getAttachmentId())
                        .set(AiParseTask::getApplicationId, request.getApplicationId())
                        .set(AiParseTask::getCandidateId, request.getCandidateId())
                        .set(AiParseTask::getModelId, resolveModelId(request.getModelId()))
                        .set(AiParseTask::getRawTextLength, textLength)
                        .set(AiParseTask::getErrorMessage, null)
                        .set(AiParseTask::getFileName, firstNonBlank(request.getFileName(), attachment.getFileName()))
                        .set(AiParseTask::getFileType, firstNonBlank(request.getFileType(), attachment.getFileType()))
                        .set(AiParseTask::getFinishedAt, LocalDateTime.now()));
    }

    private void markFailed(Long taskId, ParseTaskRequest request, String reason, int textLength) {
        AiParseTask update = new AiParseTask();
        update.setId(taskId);
        update.setTaskStatus(STATUS_FAILED);
        update.setResumeId(request.getResumeId());
        update.setAttachmentId(request.getAttachmentId());
        update.setApplicationId(request.getApplicationId());
        update.setCandidateId(request.getCandidateId());
        update.setRawTextLength(textLength);
        update.setErrorMessage(truncate(reason));
        update.setFinishedAt(LocalDateTime.now());
        parseTaskMapper.updateById(update);
    }

    /** 解析成功后触发人岗匹配；失败不影响解析主流程 */
    private void triggerMatchAfterParse(Long taskId, ParseTaskRequest request, Long resumeId) {
        if (request.getApplicationId() == null) {
            return;
        }
        try {
            Long jobId = resolveJobId(request.getApplicationId());
            if (jobId == null) {
                log.warn(
                        "解析完成后触发匹配跳过：未找到 jobId taskId={} applicationId={}",
                        taskId,
                        request.getApplicationId());
                return;
            }
            MatchRequest matchRequest = new MatchRequest();
            matchRequest.setApplicationId(request.getApplicationId());
            matchRequest.setJobId(jobId);
            matchRequest.setResumeId(resumeId);
            matchRequest.setModelId(request.getModelId());
            matchRequest.setParseTaskId(taskId);
            aiMatchService.submitMatch(matchRequest);
            log.info(
                    "解析完成后已触发人岗匹配 taskId={} applicationId={} jobId={} resumeId={}",
                    taskId,
                    request.getApplicationId(),
                    jobId,
                    resumeId);
        } catch (Exception e) {
            log.warn(
                    "解析完成后触发人岗匹配失败 taskId={} applicationId={} reason={}",
                    taskId,
                    request.getApplicationId(),
                    e.getMessage());
        }
    }

    private Long resolveJobId(Long applicationId) {
        Map<String, Object> res = jobFeignClient.getApplicationById(applicationId);
        if (res == null) {
            return null;
        }
        Object code = res.get("code");
        if (code instanceof Number codeNum && codeNum.intValue() != 200) {
            return null;
        }
        Object data = res.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return longVal(dataMap.get("jobId"));
        }
        return longVal(res.get("jobId"));
    }

    private Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long resolveModelId(Long requestModelId) {
        if (requestModelId != null) {
            return requestModelId;
        }
        AiModel model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getModelCode, DEFAULT_MODEL_CODE).last("LIMIT 1"),
                false);
        return model != null ? model.getId() : null;
    }

    private Long firstNonNull(Long primary, Long fallback) {
        return primary != null ? primary : fallback;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
