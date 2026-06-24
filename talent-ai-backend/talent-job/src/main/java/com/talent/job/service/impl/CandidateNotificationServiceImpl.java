package com.talent.job.service.impl;

import com.talent.job.entity.JobApplication;
import com.talent.job.feign.AuthFeignClient;
import com.talent.job.service.CandidateNotificationService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateNotificationServiceImpl implements CandidateNotificationService {

    private final AuthFeignClient authFeignClient;

    @Override
    public void notifyApplicationSubmitted(JobApplication application) {
        if (application == null || application.getCandidateId() == null) {
            return;
        }
        String jobTitle = application.getJobTitle() != null ? application.getJobTitle() : "岗位";
        send(
                application.getCandidateId(),
                "投递成功",
                "您已成功投递「" + jobTitle + "」，请耐心等待 HR 初筛。",
                (byte) 2,
                "application",
                application.getId());
    }

    @Override
    public void notifyScreenStatusChanged(JobApplication application, int screenStatus) {
        if (application == null || application.getCandidateId() == null) {
            return;
        }
        String jobTitle = application.getJobTitle() != null ? application.getJobTitle() : "岗位";
        String title;
        String content;
        byte notifyType = 2;
        switch (screenStatus) {
            case 2 -> {
                title = "进入面试环节";
                content = "您投递的「" + jobTitle + "」已进入面试环节，请关注后续安排。";
                notifyType = 1;
            }
            case 3 -> {
                title = "恭喜，已获得录用";
                content = "您投递的「" + jobTitle + "」已被标记为录用，请留意 Offer 相关通知。";
            }
            case 4 -> {
                title = "投递未通过";
                content = "您投递的「" + jobTitle + "」本次未通过筛选，欢迎继续关注其他岗位。";
            }
            default -> {
                title = "简历进入待初筛";
                content = "您投递的「" + jobTitle + "」已进入待初筛队列。";
            }
        }
        send(application.getCandidateId(), title, content, notifyType, "application", application.getId());
    }

    private void send(Long userId, String title, String content, byte notifyType, String bizType, Long bizId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("title", title);
            body.put("content", content);
            body.put("notifyType", notifyType);
            body.put("bizType", bizType);
            body.put("bizId", bizId);
            Map<String, Object> res = authFeignClient.createNotification(body);
            if (res == null) {
                log.warn("创建候选人通知无响应 userId={} title={}", userId, title);
                return;
            }
            Object code = res.get("code");
            if (!(code instanceof Number num) || num.intValue() != 200) {
                log.warn("创建候选人通知失败 userId={} title={} msg={}", userId, title, res.get("msg"));
            }
        } catch (Exception e) {
            log.warn("创建候选人通知异常 userId={} title={} reason={}", userId, title, e.getMessage());
        }
    }
}
