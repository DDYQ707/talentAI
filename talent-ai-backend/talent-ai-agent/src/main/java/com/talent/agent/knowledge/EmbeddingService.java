package com.talent.agent.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.config.DashScopeProperties;
import com.talent.agent.exception.AgentBusinessException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final DashScopeProperties dashScopeProperties;
    private final ObjectMapper objectMapper;

    public float[] embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("embedding 文本不能为空");
        }
        if (!StringUtils.hasText(dashScopeProperties.getApiKey())) {
            throw new AgentBusinessException("DASHSCOPE_API_KEY 未配置，无法生成向量");
        }
        try {
            EmbeddingModel model = buildEmbeddingModel();
            Embedding embedding = model.embed(text.trim()).content();
            return embedding.vector();
        } catch (AgentBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding failed", e);
            throw new AgentBusinessException("向量生成失败：" + e.getMessage(), e);
        }
    }

    public String toJson(float[] vector) {
        try {
            List<Float> values = new ArrayList<>(vector.length);
            for (float v : vector) {
                values.add(v);
            }
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new AgentBusinessException("向量序列化失败", e);
        }
    }

    public float[] fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new float[0];
        }
        try {
            List<Double> values = objectMapper.readValue(json, new TypeReference<List<Double>>() {});
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            throw new AgentBusinessException("向量反序列化失败", e);
        }
    }

    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private EmbeddingModel buildEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(dashScopeProperties.getBaseUrl())
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getEmbeddingModel())
                .timeout(Duration.ofSeconds(dashScopeProperties.getTimeoutSeconds()))
                .build();
    }
}
