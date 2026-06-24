package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class ParseTaskRequest {

    private Long attachmentId;

    private Long resumeId;

    private Long applicationId;

    private Long candidateId;

    /** 预览匹配：解析完成后自动触发人岗匹配（无投递单时使用） */
    private Long jobId;

    private Long modelId;

    private String fileName;

    private String fileType;

    /** attachment-附件 | online-在线 | merged-附件+在线合并 */
    private String parseSource;
}
