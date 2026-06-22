package com.talent.agent.chat.tool;

import com.talent.agent.client.JobFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobQueryTool {

    private final JobFeignClient jobFeignClient;
    private final ToolJsonHelper jsonHelper;

    @Tool("搜索在招岗位列表，可按岗位名称关键词过滤；关键词留空则返回全部在招岗位")
    public String searchJobPosts(@P("岗位名称关键词，可为空") String keyword) {
        Map<String, Object> response =
                jobFeignClient.listJobPosts(ToolParamHelper.normalizeKeyword(keyword), 1, 20, (byte) 1);
        return jsonHelper.fromFeign(response);
    }

    @Tool("按岗位ID查询岗位JD详情，含职责、要求、技能标签等")
    public String getJobDetail(@P("岗位ID，必填") String jobId) {
        Long id = ToolParamHelper.parseLong(jobId);
        if (id == null || id <= 0) {
            return "jobId 无效，请提供数字类型的岗位ID";
        }
        Map<String, Object> response = jobFeignClient.getJobPostBrief(id);
        return jsonHelper.fromFeign(response);
    }
}
