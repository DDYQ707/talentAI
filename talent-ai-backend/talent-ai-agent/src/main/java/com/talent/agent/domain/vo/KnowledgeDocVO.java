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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
