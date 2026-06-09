package com.talent.auth.controller;

import com.talent.auth.service.HrStaffService;
import com.talent.auth.vo.InterviewerOptionVO;
import com.talent.common.api.R;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/hr")
@RequiredArgsConstructor
public class HrStaffController {

    private final HrStaffService hrStaffService;

    @GetMapping("/interviewers")
    public R<List<InterviewerOptionVO>> listInterviewers(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        try {
            return R.ok(hrStaffService.listInterviewers(role));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
