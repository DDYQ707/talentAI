package com.talent.resume.controller;

import com.talent.common.api.R;
import com.talent.resume.dto.OnlineResumeSaveRequest;
import com.talent.resume.service.OnlineResumeService;
import com.talent.resume.vo.OnlineResumeDetailVO;
import com.talent.resume.vo.OnlineResumeListVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resume/online")
@RequiredArgsConstructor
public class OnlineResumeController {

    private final OnlineResumeService onlineResumeService;

    @GetMapping("/my")
    public R<List<OnlineResumeListVO>> myOnlineResumes(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(onlineResumeService.listMyOnlineResumes(userId));
    }

    @GetMapping("/{id}")
    public R<OnlineResumeDetailVO> detail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") Long id) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            return R.ok(onlineResumeService.getDetail(userId, id));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping
    public R<OnlineResumeDetailVO> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody(required = false) OnlineResumeSaveRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            return R.ok(onlineResumeService.create(userId, request != null ? request : new OnlineResumeSaveRequest()));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public R<OnlineResumeDetailVO> update(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") Long id,
            @RequestBody OnlineResumeSaveRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        if (request == null) {
            return R.fail("请求体不能为空");
        }
        try {
            return R.ok(onlineResumeService.update(userId, id, request));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") Long id) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            onlineResumeService.delete(userId, id);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
