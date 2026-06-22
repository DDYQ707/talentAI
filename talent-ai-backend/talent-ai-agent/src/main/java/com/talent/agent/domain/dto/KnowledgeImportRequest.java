package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class KnowledgeImportRequest {

    private String title;

    /** policy / faq / jd_template / process */
    private String category;

    private String content;

    private String sourcePath;
}
