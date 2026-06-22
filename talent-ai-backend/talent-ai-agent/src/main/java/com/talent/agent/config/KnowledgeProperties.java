package com.talent.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.knowledge")
public class KnowledgeProperties {

    private int chunkSize = 500;
    private int chunkOverlap = 80;
    private int topK = 5;
    private double minScore = 0.35;
}
