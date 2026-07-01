package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class KnowledgeUpdateRequest {

    private String title;

    /** policy / faq / jd_template / process */
    private String category;

    private String content;

    private String sourcePath;

    /** 保存后是否重新切分并向量化，默认 true */
    private Boolean reindex;
}
