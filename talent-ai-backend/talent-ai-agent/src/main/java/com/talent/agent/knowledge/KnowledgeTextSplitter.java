package com.talent.agent.knowledge;

import com.talent.agent.config.KnowledgeProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class KnowledgeTextSplitter {

    private final KnowledgeProperties knowledgeProperties;

    public List<String> split(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").trim();
        int chunkSize = Math.max(200, knowledgeProperties.getChunkSize());
        int overlap = Math.max(0, Math.min(knowledgeProperties.getChunkOverlap(), chunkSize / 2));

        List<String> paragraphs = splitParagraphs(normalized);
        List<String> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (!StringUtils.hasText(paragraph)) {
                continue;
            }
            if (buffer.length() + paragraph.length() + 1 <= chunkSize) {
                if (buffer.length() > 0) {
                    buffer.append('\n');
                }
                buffer.append(paragraph.trim());
                continue;
            }
            if (buffer.length() > 0) {
                chunks.add(buffer.toString());
                buffer = new StringBuilder(overlapTail(buffer.toString(), overlap));
            }
            if (paragraph.length() <= chunkSize) {
                if (buffer.length() > 0) {
                    buffer.append('\n');
                }
                buffer.append(paragraph.trim());
            } else {
                chunks.addAll(splitLongText(paragraph.trim(), chunkSize, overlap));
            }
        }
        if (buffer.length() > 0) {
            chunks.add(buffer.toString());
        }
        return chunks.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private List<String> splitParagraphs(String text) {
        String[] parts = text.split("\n\\s*\n");
        List<String> paragraphs = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                paragraphs.add(part.trim());
            }
        }
        return paragraphs;
    }

    private List<String> splitLongText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            chunks.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private String overlapTail(String text, int overlap) {
        if (overlap <= 0 || text.length() <= overlap) {
            return "";
        }
        return text.substring(text.length() - overlap);
    }
}
