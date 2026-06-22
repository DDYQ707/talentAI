package com.talent.agent.chat.tool;

import com.talent.agent.chat.context.ChatCandidateContext;
import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.service.AiMatchService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchQueryTool {

    private final AiMatchService aiMatchService;
    private final RecruitLookupSupport recruitLookupSupport;
    private final ResumeFeignClient resumeFeignClient;
    private final ChatCandidateContext chatCandidateContext;
    private final ToolJsonHelper jsonHelper;

    @Tool("按投递单ID查询AI人岗匹配结果，返回匹配分、等级、优劣势、维度分、建议面试题等")
    public String getMatchByApplication(@P("投递单ID，必填") String applicationId) {
        Long id = ToolParamHelper.parseLong(applicationId);
        if (id == null || id <= 0) {
            return "applicationId 无效，请提供数字类型的投递单ID";
        }
        MatchResultVO result = aiMatchService.getByApplicationId(id);
        if (result == null) {
            return "未找到投递单 " + id + " 的匹配记录，可能尚未完成 AI 匹配";
        }
        registerCandidateFromMatch(result);
        return jsonHelper.toJson(result);
    }

    @Tool("按候选人姓名查询其最近投递的AI人岗匹配结果（含匹配分、优劣势等）")
    public String getMatchByCandidateName(@P("候选人姓名，必填") String candidateName) {
        return recruitLookupSupport.lookupMatchByCandidateName(candidateName);
    }

    @Tool("按简历ID和岗位ID查询最新一条AI人岗匹配结果")
    public String getMatchByResumeAndJob(
            @P("简历ID，必填") String resumeId,
            @P("岗位ID，必填") String jobId) {
        Long parsedResumeId = ToolParamHelper.parseLong(resumeId);
        Long parsedJobId = ToolParamHelper.parseLong(jobId);
        if (parsedResumeId == null || parsedResumeId <= 0 || parsedJobId == null || parsedJobId <= 0) {
            return "resumeId 与 jobId 必须为有效数字";
        }
        MatchResultVO result = aiMatchService.getLatestByResumeAndJob(parsedResumeId, parsedJobId);
        if (result == null) {
            return "未找到简历 " + parsedResumeId + " 与岗位 " + parsedJobId + " 的匹配记录";
        }
        registerCandidateFromMatch(result);
        return jsonHelper.toJson(result);
    }

    @SuppressWarnings("unchecked")
    private void registerCandidateFromMatch(MatchResultVO result) {
        if (result == null || result.getResumeId() == null) {
            return;
        }
        Map<String, Object> briefResponse = resumeFeignClient.getHrResumeBrief(result.getResumeId());
        if (briefResponse != null && briefResponse.get("data") instanceof Map<?, ?> dataMap) {
            chatCandidateContext.addFromResumeRecord((Map<String, Object>) dataMap);
        }
        chatCandidateContext.enrichMatch(
                result.getResumeId(), result.getApplicationId(), result.getMatchScore());
    }
}
