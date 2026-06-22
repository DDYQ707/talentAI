package com.talent.job.controller;

import com.talent.common.api.R;
import com.talent.job.dto.SyncScreenStatusRequest;
import com.talent.job.service.IJobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 投递申请表（talent-job库） 前端控制器
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */

@RestController
@RequestMapping("/jobApplication") // 记得确认网关是不是能正确路由到这里
public class JobApplicationController {

//    @Autowired
//    private IJobApplicationService jobApplicationService;
//
//    /**
//     * 更新候选人投递状态 (触发下游 Offer 或 人才库 联动)
//     * PUT /jobApplication/{id}/status
//     */
//    @PutMapping("/{id}/status")
//    public R<Void> updateApplicationStatus(
//            @PathVariable("id") Long applicationId,
//            @RequestParam("status") Integer status,
//            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
//
//        if (userId == null) {
//            return R.fail("未检测到登录用户信息，拒绝操作");
//        }
//
//        SyncScreenStatusRequest syncReq = new SyncScreenStatusRequest();
//
//        syncReq.setCandidateId(applicationId);
//
//        syncReq.setOperatorId(userId);
//
//        syncReq.setScreenStatus(status);
//
//        jobApplicationService.syncLatestApplicationByScreenStatus(syncReq);
//
//        return R.ok();
//    }
}