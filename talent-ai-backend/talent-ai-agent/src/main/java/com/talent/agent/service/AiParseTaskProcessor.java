package com.talent.agent.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.dto.ResumeAttachmentInfo;
import com.talent.agent.domain.entity.AiParseTask;
import com.talent.agent.mapper.AiParseTaskMapper;
import java.time.LocalDateTime;
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

    private final AiParseTaskMapper parseTaskMapper;
    private final ResumeAttachmentQueryService attachmentQueryService;
    private final MinioDownloadService minioDownloadService;
    private final ResumeTextExtractService textExtractService;

    @Async
    public void processAsync(Long taskId, ParseTaskRequest request) {
        markProcessing(taskId);
        try {
            ResumeAttachmentInfo attachment = attachmentQueryService.fetchByAttachmentId(request.getAttachmentId());
            byte[] fileBytes = minioDownloadService.download(attachment.getBucketName(), attachment.getObjectKey());
            ResumeTextExtractService.ExtractResult result = textExtractService.extract(
                    fileBytes, attachment.getFileName(), attachment.getFileType());

            markSuccess(taskId, request, attachment, result.length());
            log.info(
                    "简历文本抽取成功 taskId={} attachmentId={} resumeId={} fileName={} textLength={}",
                    taskId,
                    request.getAttachmentId(),
                    request.getResumeId(),
                    attachment.getFileName(),
                    result.length());
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            markFailed(taskId, request, reason);
            log.warn(
                    "简历文本抽取失败 taskId={} attachmentId={} resumeId={} reason={}",
                    taskId,
                    request.getAttachmentId(),
                    request.getResumeId(),
                    truncate(reason));
        }
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
                        .set(AiParseTask::getRawTextLength, textLength)
                        .set(AiParseTask::getErrorMessage, null)
                        .set(AiParseTask::getFileName, firstNonBlank(request.getFileName(), attachment.getFileName()))
                        .set(AiParseTask::getFileType, firstNonBlank(request.getFileType(), attachment.getFileType()))
                        .set(AiParseTask::getFinishedAt, LocalDateTime.now()));
    }

    private void markFailed(Long taskId, ParseTaskRequest request, String reason) {
        AiParseTask update = new AiParseTask();
        update.setId(taskId);
        update.setTaskStatus(STATUS_FAILED);
        update.setResumeId(request.getResumeId());
        update.setAttachmentId(request.getAttachmentId());
        update.setApplicationId(request.getApplicationId());
        update.setCandidateId(request.getCandidateId());
        update.setRawTextLength(0);
        update.setErrorMessage(truncate(reason));
        update.setFinishedAt(LocalDateTime.now());
        parseTaskMapper.updateById(update);
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
