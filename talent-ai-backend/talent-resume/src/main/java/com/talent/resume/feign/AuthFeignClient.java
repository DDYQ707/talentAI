package com.talent.resume.feign;

import com.talent.resume.dto.ParseProfilePatchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-auth")
public interface AuthFeignClient {

    @GetMapping("/api/auth/getUserName")
    String getUserName(@RequestParam("id") Long id);

  /** 内部：候选人档案摘要（HR 简历详情） */
    @GetMapping("/api/auth/internal/candidateBrief")
    java.util.Map<String, Object> getCandidateProfileBrief(@RequestParam("userId") Long userId);

    /** 内部：AI 解析结果回填候选人档案（仅补空字段） */
    @PostMapping("/api/auth/internal/patch-from-parse")
    java.util.Map<String, Object> patchProfileFromParse(@RequestBody ParseProfilePatchRequest request);
}
