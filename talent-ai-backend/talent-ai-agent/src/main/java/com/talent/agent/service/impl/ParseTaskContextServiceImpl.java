package com.talent.agent.service.impl;

import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.service.ParseTaskContextService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParseTaskContextServiceImpl implements ParseTaskContextService {

    private final ResumeFeignClient resumeFeignClient;

    @Override
    public ParseTaskRequest buildFromResumeContext(
            Long resumeId, Long applicationId, Long jobId, Long candidateId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }

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
        Long resolvedCandidateId = candidateId != null ? candidateId : longVal(ctx.get("candidateId"));

        ParseTaskRequest request = new ParseTaskRequest();
        request.setResumeId(targetResumeId);
        request.setApplicationId(applicationId);
        request.setJobId(jobId);
        request.setCandidateId(resolvedCandidateId);

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
        return request;
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
