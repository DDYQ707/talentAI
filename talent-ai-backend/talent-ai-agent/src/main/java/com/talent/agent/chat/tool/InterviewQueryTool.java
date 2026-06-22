package com.talent.agent.chat.tool;

import com.talent.agent.client.InterviewFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewQueryTool {

    private final InterviewFeignClient interviewFeignClient;
    private final ToolJsonHelper jsonHelper;

    @Tool("按投递单ID查询关联的面试安排列表，含时间、状态、面试官等")
    public String listInterviewsByApplication(@P("投递单ID，必填") String applicationId) {
        Long id = ToolParamHelper.parseLong(applicationId);
        if (id == null || id <= 0) {
            return "applicationId 无效，请提供数字类型的投递单ID";
        }
        Map<String, Object> response = interviewFeignClient.listByApplication(id);
        return jsonHelper.fromFeign(response);
    }

    @Tool("按面试ID查询单场面试详情摘要")
    public String getInterviewBrief(@P("面试ID，必填") String interviewId) {
        Long id = ToolParamHelper.parseLong(interviewId);
        if (id == null || id <= 0) {
            return "interviewId 无效，请提供数字类型的面试ID";
        }
        Map<String, Object> response = interviewFeignClient.getBrief(id);
        return jsonHelper.fromFeign(response);
    }
}
