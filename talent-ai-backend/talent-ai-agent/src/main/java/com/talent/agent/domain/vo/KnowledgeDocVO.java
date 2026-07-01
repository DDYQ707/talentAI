package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeDocVO {

    private Long id;

    private String title;

    private String category;

    private String sourcePath;

    private Integer status;

    private Integer chunkCount;

    /** 详情接口返回正文，列表接口不返回 */
    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
