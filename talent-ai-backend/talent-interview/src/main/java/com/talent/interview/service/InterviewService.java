package com.talent.interview.service;

import com.talent.interview.dto.InterviewEvaluationRequest;
import com.talent.interview.dto.InterviewScheduleRequest;
import com.talent.interview.entity.Interview;
import com.talent.interview.vo.InterviewBriefVO;
import com.talent.interview.vo.InterviewDetailVO;
import com.talent.interview.vo.InterviewEvaluationVO;
import com.talent.interview.vo.InterviewListVO;
import com.talent.interview.vo.InterviewScheduleResultVO;
import com.talent.interview.vo.InterviewStatsVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface InterviewService {

    Interview getById(Long interviewId);

    List<Interview> listByApplicationId(Long applicationId);

    List<InterviewEvaluationVO> listEvaluationsByApplication(Long applicationId);

    InterviewBriefVO toBrief(Interview interview);

    InterviewScheduleResultVO schedule(
            String role, Long hrId, String hrName, InterviewScheduleRequest request);

    Map<String, Object> pageForHr(
            String role, Integer page, Integer size, String keyword, Integer status,
            LocalDate dateFrom, LocalDate dateTo);

    InterviewStatsVO statsForHr(String role);

    InterviewDetailVO detailForHr(String role, Long interviewId);

    void cancelForHr(String role, Long interviewId);

    void reassignForHr(String role, Long hrId, String hrName, Long interviewId, Long interviewerId);

    List<InterviewListVO> listByApplicationForHr(String role, Long applicationId);

    Map<String, Object> pageForInterviewer(
            String role, Long interviewerId, Integer page, Integer size, String keyword, Integer status);

    InterviewStatsVO statsForInterviewer(String role, Long interviewerId);

    InterviewDetailVO detailForInterviewer(String role, Long interviewerId, Long interviewId);

    Map<String, Object> pageForCandidate(
            String role, Long candidateId, Integer page, Integer size, String keyword, Integer status);

    InterviewDetailVO detailForCandidate(String role, Long candidateId, Long interviewId);

    /** HR 淘汰候选人后联动：取消该投递下未完成的面试安排 */
    void syncOnApplicationRejected(Long applicationId);
}
