package com.talent.agent.chat.tool;

import com.talent.agent.client.JobFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationQueryTool {

    private final JobFeignClient jobFeignClient;
    private final ToolJsonHelper jsonHelper;

    @Tool("按投递单ID查询投递记录，含候选人姓名、岗位、匹配分、当前阶段等")
    public String getApplicationById(@P("投递单ID，必填") String applicationId) {
        Long id = ToolParamHelper.parseLong(applicationId);
        if (id == null || id <= 0) {
            return "applicationId 无效，请提供数字类型的投递单ID";
        }
        Map<String, Object> response = jobFeignClient.getApplicationById(id);
        return jsonHelper.fromFeign(response);
    }

    @Tool("按简历ID查询该候选人最近一条投递记录")
    public String getLatestApplicationByResume(@P("简历ID，必填") String resumeId) {
        Long id = ToolParamHelper.parseLong(resumeId);
        if (id == null || id <= 0) {
            return "resumeId 无效，请提供数字类型的简历ID";
        }
        Map<String, Object> response = jobFeignClient.getLatestApplicationByResume(id);
        if (response == null || response.isEmpty()) {
            return "简历 " + id + " 暂无投递记录";
        }
        return jsonHelper.toJson(response);
    }
}
