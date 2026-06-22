package com.talent.agent.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.config.KnowledgeProperties;
import com.talent.agent.domain.entity.AiKnowledgeChunk;
import com.talent.agent.domain.entity.AiKnowledgeDoc;
import com.talent.agent.domain.vo.KnowledgeHitVO;
import com.talent.agent.mapper.AiKnowledgeChunkMapper;
import com.talent.agent.mapper.AiKnowledgeDocMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final KnowledgeProperties knowledgeProperties;
    private final ObjectMapper objectMapper;

    public String search(String query) {
        return search(query, null);
    }

    public String search(String query, String category) {
        if (!StringUtils.hasText(query)) {
            return "请提供检索问题";
        }
        List<KnowledgeHitVO> hits = retrieve(query.trim(), category, knowledgeProperties.getTopK());
        if (hits.isEmpty()) {
            return "知识库中未找到与「" + query.trim() + "」相关的内容";
        }
        try {
            return objectMapper.writeValueAsString(hits);
        } catch (Exception e) {
            throw new IllegalStateException("检索结果序列化失败", e);
        }
    }

    public List<KnowledgeHitVO> retrieve(String query, String category, int topK) {
        float[] queryVector = embeddingService.embed(query);
        List<AiKnowledgeChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeChunk>()
                        .isNotNull(AiKnowledgeChunk::getEmbeddingJson)
                        .orderByAsc(AiKnowledgeChunk::getDocId)
                        .orderByAsc(AiKnowledgeChunk::getChunkIndex));
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, AiKnowledgeDoc> docCache = new HashMap<>();
        List<ScoredHit> scored = new ArrayList<>();
        for (AiKnowledgeChunk chunk : chunks) {
            AiKnowledgeDoc doc = loadDoc(docCache, chunk.getDocId());
            if (doc == null || doc.getStatus() == null || doc.getStatus() != 1) {
                continue;
            }
            if (StringUtils.hasText(category) && !category.equalsIgnoreCase(doc.getCategory())) {
                continue;
            }
            float[] vector = embeddingService.fromJson(chunk.getEmbeddingJson());
            double score = embeddingService.cosineSimilarity(queryVector, vector);
            if (score < knowledgeProperties.getMinScore()) {
                continue;
            }
            KnowledgeHitVO hit = new KnowledgeHitVO();
            hit.setDocId(doc.getId());
            hit.setTitle(doc.getTitle());
            hit.setCategory(doc.getCategory());
            hit.setSnippet(chunk.getContent());
            hit.setScore(Math.round(score * 1000.0) / 1000.0);
            scored.add(new ScoredHit(score, hit));
        }

        scored.sort(Comparator.comparingDouble(ScoredHit::score).reversed());
        int limit = topK > 0 ? topK : knowledgeProperties.getTopK();
        return scored.stream().limit(limit).map(item -> item.hit()).toList();
    }

    private AiKnowledgeDoc loadDoc(Map<Long, AiKnowledgeDoc> cache, Long docId) {
        if (docId == null) {
            return null;
        }
        return cache.computeIfAbsent(docId, docMapper::selectById);
    }

    private record ScoredHit(double score, KnowledgeHitVO hit) {
    }
}
