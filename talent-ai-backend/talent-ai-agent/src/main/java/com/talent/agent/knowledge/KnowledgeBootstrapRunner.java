package com.talent.agent.knowledge;

import com.talent.agent.domain.dto.KnowledgeImportRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBootstrapRunner {

    private final KnowledgeIngestService knowledgeIngestService;
    private final KnowledgeMarkdownLoader knowledgeMarkdownLoader;

    @Order(20)
    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        if (knowledgeIngestService.countDocs() > 0) {
            log.info("Knowledge base already initialized, skip seed");
            return;
        }
        List<KnowledgeImportRequest> seeds = knowledgeMarkdownLoader.loadSeedDocuments();
        if (seeds.isEmpty()) {
            log.warn("No knowledge markdown seeds found under classpath:knowledge/*.md");
            return;
        }
        log.info("Knowledge base empty, seeding {} markdown documents...", seeds.size());
        for (KnowledgeImportRequest request : seeds) {
            try {
                knowledgeIngestService.importDocument(request);
            } catch (Exception e) {
                log.warn("Knowledge seed failed for title={}, reason={}", request.getTitle(), e.getMessage());
            }
        }
    }
}
