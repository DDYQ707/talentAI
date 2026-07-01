package com.talent.agent.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.domain.dto.KnowledgeImportRequest;
import com.talent.agent.domain.dto.KnowledgeUpdateRequest;
import com.talent.agent.domain.entity.AiKnowledgeChunk;
import com.talent.agent.domain.entity.AiKnowledgeDoc;
import com.talent.agent.domain.vo.KnowledgeDocVO;
import com.talent.agent.mapper.AiKnowledgeChunkMapper;
import com.talent.agent.mapper.AiKnowledgeDocMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestService {

    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final KnowledgeTextSplitter textSplitter;
    private final EmbeddingService embeddingService;

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocVO importDocument(KnowledgeImportRequest request) {
        validateImportRequest(request);
        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setTitle(request.getTitle().trim());
        doc.setCategory(normalizeCategory(request.getCategory()));
        doc.setContent(request.getContent().trim());
        doc.setSourcePath(request.getSourcePath());
        doc.setStatus(1);
        docMapper.insert(doc);
        indexDocument(doc);
        return toVo(doc, countChunks(doc.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocVO reindex(Long docId) {
        AiKnowledgeDoc doc = requireDoc(docId);
        deleteChunks(docId);
        indexDocument(doc);
        return toVo(doc, countChunks(docId));
    }

    public KnowledgeDocVO getDocDetail(Long docId) {
        AiKnowledgeDoc doc = requireDoc(docId);
        KnowledgeDocVO vo = toVo(doc, countChunks(doc.getId()));
        vo.setContent(doc.getContent());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocVO updateDocument(Long docId, KnowledgeUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        AiKnowledgeDoc doc = requireDoc(docId);
        if (StringUtils.hasText(request.getTitle())) {
            doc.setTitle(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getCategory())) {
            doc.setCategory(normalizeCategory(request.getCategory()));
        }
        if (request.getContent() != null) {
            String content = request.getContent().trim();
            if (!StringUtils.hasText(content)) {
                throw new IllegalArgumentException("content 不能为空");
            }
            doc.setContent(content);
        }
        if (request.getSourcePath() != null) {
            doc.setSourcePath(StringUtils.hasText(request.getSourcePath()) ? request.getSourcePath().trim() : null);
        }
        docMapper.updateById(doc);

        boolean shouldReindex = request.getReindex() == null || Boolean.TRUE.equals(request.getReindex());
        if (shouldReindex) {
            deleteChunks(docId);
            indexDocument(doc);
        }
        KnowledgeDocVO vo = toVo(doc, countChunks(docId));
        vo.setContent(doc.getContent());
        return vo;
    }

    public List<KnowledgeDocVO> listDocs() {
        List<AiKnowledgeDoc> docs = docMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDoc>()
                        .eq(AiKnowledgeDoc::getStatus, 1)
                        .orderByDesc(AiKnowledgeDoc::getUpdatedAt));
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        List<KnowledgeDocVO> result = new ArrayList<>(docs.size());
        for (AiKnowledgeDoc doc : docs) {
            result.add(toVo(doc, countChunks(doc.getId())));
        }
        return result;
    }

    public long countDocs() {
        return docMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private void indexDocument(AiKnowledgeDoc doc) {
        List<String> chunks = textSplitter.split(doc.getContent());
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("文档内容切分后为空");
        }
        int index = 0;
        for (String chunkText : chunks) {
            float[] vector = embeddingService.embed(chunkText);
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setChunkIndex(index++);
            chunk.setContent(chunkText);
            chunk.setEmbeddingJson(embeddingService.toJson(vector));
            chunkMapper.insert(chunk);
        }
        log.info("Knowledge indexed, docId={}, title={}, chunks={}", doc.getId(), doc.getTitle(), chunks.size());
    }

    private void deleteChunks(Long docId) {
        chunkMapper.delete(new LambdaQueryWrapper<AiKnowledgeChunk>().eq(AiKnowledgeChunk::getDocId, docId));
    }

    private AiKnowledgeDoc requireDoc(Long docId) {
        AiKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("知识文档不存在");
        }
        return doc;
    }

    private int countChunks(Long docId) {
        Long count = chunkMapper.selectCount(
                new LambdaQueryWrapper<AiKnowledgeChunk>().eq(AiKnowledgeChunk::getDocId, docId));
        return count == null ? 0 : count.intValue();
    }

    private void validateImportRequest(KnowledgeImportRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("title 与 content 不能为空");
        }
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return "faq";
        }
        return category.trim().toLowerCase();
    }

    private KnowledgeDocVO toVo(AiKnowledgeDoc doc, int chunkCount) {
        KnowledgeDocVO vo = new KnowledgeDocVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setCategory(doc.getCategory());
        vo.setSourcePath(doc.getSourcePath());
        vo.setStatus(doc.getStatus());
        vo.setChunkCount(chunkCount);
        vo.setCreatedAt(doc.getCreatedAt());
        vo.setUpdatedAt(doc.getUpdatedAt());
        return vo;
    }
}
