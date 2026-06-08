package com.talent.agent.service;

import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.dto.ResumeAttachmentInfo;
import com.talent.agent.exception.AgentBusinessException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeAttachmentQueryService {

    private final ResumeFeignClient resumeFeignClient;

    public ResumeAttachmentInfo fetchByAttachmentId(Long attachmentId) {
        Map<String, Object> response;
        try {
            response = resumeFeignClient.getAttachmentById(attachmentId);
        } catch (Exception e) {
            throw new AgentBusinessException("查询简历附件失败: " + e.getMessage(), e);
        }
        if (response == null) {
            throw new AgentBusinessException("查询简历附件无响应");
        }
        Object codeObj = response.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 500;
        if (code != 200) {
            Object msg = response.get("msg");
            throw new AgentBusinessException(msg != null ? String.valueOf(msg) : "查询简历附件失败");
        }
        Object dataObj = response.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            throw new AgentBusinessException("简历附件数据格式异常");
        }
        ResumeAttachmentInfo info = new ResumeAttachmentInfo();
        info.setAttachmentId(toLong(dataMap.get("attachmentId")));
        info.setResumeId(toLong(dataMap.get("resumeId")));
        info.setFileName(toString(dataMap.get("fileName")));
        info.setFileType(toString(dataMap.get("fileType")));
        info.setFileSize(toLong(dataMap.get("fileSize")));
        info.setBucketName(toString(dataMap.get("bucketName")));
        info.setObjectKey(toString(dataMap.get("objectKey")));
        return info;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
