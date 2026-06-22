package com.talent.agent.chat.tool;

import com.talent.agent.knowledge.KnowledgeRetrievalService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool {

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @Tool("检索招聘制度、流程说明、FAQ、JD模板等知识库。适用于：招聘流程、筛选状态含义、AI匹配分解读、系统操作说明等非实时业务数据问题")
    public String searchKnowledge(@P("检索问题，必填") String query) {
        return knowledgeRetrievalService.search(query);
    }

    @Tool("按分类检索知识库。category 可选：policy流程制度、faq常见问题、process招聘流程、jd_template岗位模板")
    public String searchKnowledgeByCategory(
            @P("检索问题，必填") String query,
            @P("分类，必填") String category) {
        return knowledgeRetrievalService.search(query, category);
    }
}
