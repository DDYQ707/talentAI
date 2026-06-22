package com.talent.agent.chat.tool;

import com.talent.agent.chat.context.ChatCandidateContext;
import com.talent.agent.client.JobFeignClient;
import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.service.AiMatchService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RecruitLookupSupport {

    private static final int SCREEN_PENDING = 1;

    private final ResumeFeignClient resumeFeignClient;
    private final JobFeignClient jobFeignClient;
    private final AiMatchService aiMatchService;
    private final ToolJsonHelper jsonHelper;
    private final ChatCandidateContext chatCandidateContext;

    public String searchPendingResumes() {
        Map<String, Object> response =
                resumeFeignClient.searchHrResumes(null, SCREEN_PENDING, 1, 20);
        registerCandidatesFromResponse(response);
        return jsonHelper.fromFeign(response);
    }

    public String searchResumesByKeyword(String keyword) {
        String normalized = ToolParamHelper.normalizeKeyword(keyword);
        if (normalized == null) {
            return "请提供搜索关键词（候选人姓名或简历名称）";
        }
        Map<String, Object> response = resumeFeignClient.searchHrResumes(normalized, null, 1, 20);
        registerCandidatesFromResponse(response);
        return jsonHelper.fromFeign(response);
    }

    public String lookupMatchByCandidateName(String candidateName) {
        String normalized = ToolParamHelper.normalizeKeyword(candidateName);
        if (normalized == null) {
            return "请提供候选人姓名";
        }

        Map<String, Object> searchResponse =
                resumeFeignClient.searchHrResumes(normalized, null, 1, 20);
        List<Map<String, Object>> records = extractRecords(searchResponse);
        if (records.isEmpty()) {
            return "未找到姓名包含「" + normalized + "」的候选人简历";
        }

        Map<String, Object> resume = pickBestResume(records, normalized);
        chatCandidateContext.addFromResumeRecord(resume);
        Object resumeIdObj = resume.get("id");
        if (!(resumeIdObj instanceof Number resumeIdNumber)) {
            return "候选人简历数据异常，缺少 resumeId";
        }
        long resumeId = resumeIdNumber.longValue();

        Map<String, Object> applicationResponse =
                jobFeignClient.getLatestApplicationByResume(resumeId);
        if (applicationResponse == null || applicationResponse.isEmpty()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("candidateName", resume.get("candidateName"));
            summary.put("resumeId", resumeId);
            summary.put("message", "该候选人暂无投递记录，无法查询 AI 匹配结果");
            return jsonHelper.toJson(summary);
        }

        Object applicationIdObj = applicationResponse.get("applicationId");
        if (!(applicationIdObj instanceof Number applicationIdNumber)) {
            return "该候选人最近投递记录缺少 applicationId";
        }
        long applicationId = applicationIdNumber.longValue();

        MatchResultVO match = aiMatchService.getByApplicationId(applicationId);
        Integer matchScore = match != null ? match.getMatchScore() : asInteger(applicationResponse.get("matchScore"));
        chatCandidateContext.enrichMatch(resumeId, applicationId, matchScore);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidateName", resume.get("candidateName"));
        result.put("resumeId", resumeId);
        result.put("applicationId", applicationId);
        result.put("jobTitle", applicationResponse.get("jobTitle"));
        result.put("applicationMatchScore", applicationResponse.get("matchScore"));
        if (match == null) {
            result.put("message", "投递单 " + applicationId + " 暂无 AI 匹配详情，可能尚未完成匹配");
        } else {
            result.put("aiMatch", match);
        }
        return jsonHelper.toJson(result);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRecords(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() != 200) {
            return List.of();
        }
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return List.of();
        }
        Object records = dataMap.get("records");
        if (!(records instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private Map<String, Object> pickBestResume(List<Map<String, Object>> records, String keyword) {
        for (Map<String, Object> record : records) {
            Object name = record.get("candidateName");
            if (name instanceof String candidateName
                    && StringUtils.hasText(candidateName)
                    && candidateName.contains(keyword)) {
                return record;
            }
        }
        return records.get(0);
    }

    private void registerCandidatesFromResponse(Map<String, Object> response) {
        chatCandidateContext.addFromResumeRecords(extractRecords(response));
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
