package com.talent.agent.chat.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.talent.agent.client.ResumeFeignClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResumeSearchTool {

    private final ResumeFeignClient resumeFeignClient;
    private final RecruitLookupSupport recruitLookupSupport;
    private final ToolJsonHelper jsonHelper;

    @Tool("搜索待初筛状态的简历列表（screenStatus=1）")
    public String searchPendingScreenResumes() {
        return recruitLookupSupport.searchPendingResumes();
    }

    @Tool("按关键词搜索HR简历库，可匹配候选人姓名或简历名称")
    public String searchResumesByKeyword(@P("搜索关键词，必填") String keyword) {
        return recruitLookupSupport.searchResumesByKeyword(keyword);
    }

    @Tool("按简历ID查询简历详情摘要，含候选人信息、筛选状态、最近投递等")
    public String getResumeDetail(@P("简历ID，必填") String resumeId) {
        Long id = ToolParamHelper.parseLong(resumeId);
        if (id == null || id <= 0) {
            return "resumeId 无效，请提供数字类型的简历ID";
        }
        Map<String, Object> response = resumeFeignClient.getHrResumeBrief(id);
        return jsonHelper.fromFeign(response);
    }
}
