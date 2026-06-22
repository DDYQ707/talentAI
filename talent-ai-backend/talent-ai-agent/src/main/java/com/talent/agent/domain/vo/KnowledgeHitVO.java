package com.talent.agent.domain.vo;

import lombok.Data;

@Data
public class KnowledgeHitVO {

    private Long docId;

    private String title;

    private String category;

    private String snippet;

    private Double score;
}
