package com.talent.agent.knowledge;

import com.talent.agent.domain.dto.KnowledgeImportRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class KnowledgeMarkdownLoader {

    private static final Pattern FRONT_MATTER = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);
    private static final Pattern META_LINE = Pattern.compile("^([A-Za-z_][A-Za-z0-9_-]*)\\s*:\\s*(.+)$");

    public List<KnowledgeImportRequest> loadSeedDocuments() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<LoadedMarkdown> loaded = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources("classpath:knowledge/*.md");
            for (Resource resource : resources) {
                if (resource == null || !resource.exists()) {
                    continue;
                }
                String filename = resource.getFilename();
                if ("README.md".equalsIgnoreCase(filename)) {
                    continue;
                }
                loaded.add(parseResource(resource));
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取知识库 Markdown 种子失败", e);
        }
        loaded.sort(Comparator.comparing(LoadedMarkdown::filename));
        List<KnowledgeImportRequest> requests = new ArrayList<>(loaded.size());
        for (LoadedMarkdown item : loaded) {
            requests.add(toImportRequest(item));
        }
        return requests;
    }

    private LoadedMarkdown parseResource(Resource resource) throws IOException {
        String filename = resource.getFilename();
        String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Matcher matcher = FRONT_MATTER.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Markdown 缺少 front matter: " + filename);
        }
        Map<String, String> meta = parseFrontMatter(matcher.group(1));
        String title = meta.get("title");
        String category = meta.get("category");
        if (!StringUtils.hasText(title) || !StringUtils.hasText(category)) {
            throw new IllegalArgumentException("Markdown front matter 缺少 title/category: " + filename);
        }
        String content = matcher.group(2).trim();
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Markdown 正文为空: " + filename);
        }
        LoadedMarkdown loaded = new LoadedMarkdown(
                filename == null ? "" : filename,
                title.trim(),
                category.trim().toLowerCase(),
                "classpath:knowledge/" + filename,
                content);
        return loaded;
    }

    private Map<String, String> parseFrontMatter(String block) {
        Map<String, String> meta = new java.util.LinkedHashMap<>();
        for (String line : block.split("\\R")) {
            Matcher matcher = META_LINE.matcher(line.trim());
            if (matcher.matches()) {
                meta.put(matcher.group(1).trim().toLowerCase(), matcher.group(2).trim());
            }
        }
        return meta;
    }

    private KnowledgeImportRequest toImportRequest(LoadedMarkdown loaded) {
        KnowledgeImportRequest request = new KnowledgeImportRequest();
        request.setTitle(loaded.title());
        request.setCategory(loaded.category());
        request.setSourcePath(loaded.sourcePath());
        request.setContent(loaded.content());
        return request;
    }

    private record LoadedMarkdown(String filename, String title, String category, String sourcePath, String content) {
    }
}
