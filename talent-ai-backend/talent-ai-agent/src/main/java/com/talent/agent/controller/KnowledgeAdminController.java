package com.talent.agent.controller;

import com.talent.agent.domain.dto.KnowledgeImportRequest;
import com.talent.agent.domain.vo.KnowledgeDocVO;
import com.talent.agent.knowledge.KnowledgeIngestService;
import com.talent.agent.util.ChatAuthSupport;
import com.talent.common.api.R;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeAdminController {

    private final KnowledgeIngestService knowledgeIngestService;

    @GetMapping("/docs")
    public R<List<KnowledgeDocVO>> listDocs(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        ChatAuthSupport.requireHr(role);
        return R.ok(knowledgeIngestService.listDocs());
    }

    @PostMapping("/import")
    public R<KnowledgeDocVO> importDoc(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody KnowledgeImportRequest request) {
        ChatAuthSupport.requireHr(role);
        try {
            return R.ok(knowledgeIngestService.importDocument(request));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/{docId}/reindex")
    public R<KnowledgeDocVO> reindex(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long docId) {
        ChatAuthSupport.requireHr(role);
        try {
            return R.ok(knowledgeIngestService.reindex(docId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
