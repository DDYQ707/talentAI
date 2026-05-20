package com.talent.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.auth.dto.CandidateProfileSaveRequest;
import com.talent.auth.entity.CandidateProfile;
import com.talent.auth.entity.SysUser;
import com.talent.auth.vo.CandidateProfileCompletenessVO;
import com.talent.auth.vo.CandidateProfileVO;
import com.talent.common.api.R;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CandidateMyProfileService {

    private static final byte USER_TYPE_CANDIDATE = 1;

    private final ISysUserService sysUserService;
    private final ICandidateProfileService candidateProfileService;

    public R<CandidateProfileVO> getMyProfile(Long userId) {
        R<SysUser> userResult = requireCandidateUser(userId);
        if (userResult.getCode() != 200) {
            return R.fail(userResult.getMsg());
        }
        CandidateProfile profile = getOrCreateProfile(userId);
        CandidateProfileVO vo = toVO(userResult.getData(), profile);
        CandidateProfileCompletenessVO completeness = evaluateCompleteness(userResult.getData(), profile);
        vo.setProfileComplete(completeness.isComplete());
        vo.setResumeCompleteness(completeness.getCompleteness());
        return R.ok(vo);
    }

    public R<CandidateProfileCompletenessVO> getCompleteness(Long userId) {
        R<SysUser> userResult = requireCandidateUser(userId);
        if (userResult.getCode() != 200) {
            return R.fail(userResult.getMsg());
        }
        CandidateProfile profile = getOrCreateProfile(userId);
        return R.ok(evaluateCompleteness(userResult.getData(), profile));
    }

    @Transactional(rollbackFor = Exception.class)
    public R<CandidateProfileVO> saveMyProfile(Long userId, CandidateProfileSaveRequest request) {
        R<SysUser> userResult = requireCandidateUser(userId);
        if (userResult.getCode() != 200) {
            return R.fail(userResult.getMsg());
        }
        if (request == null) {
            return R.fail("请求体不能为空");
        }

        String realName = trim(request.getRealName());
        String phone = trim(request.getPhone());
        String email = trim(request.getEmail());
        String city = trim(request.getCity());
        Byte highestEdu = request.getHighestEdu();

        if (!StringUtils.hasText(realName)) {
            return R.fail("请填写真实姓名");
        }
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            return R.fail("请填写正确的手机号");
        }
        if (StringUtils.hasText(email) && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return R.fail("邮箱格式不正确");
        }
        if (!StringUtils.hasText(city)) {
            return R.fail("请填写所在城市");
        }
        if (highestEdu == null || highestEdu < 1 || highestEdu > 4) {
            return R.fail("请选择最高学历");
        }

        SysUser user = userResult.getData();
        user.setPhone(phone);
        if (StringUtils.hasText(email)) {
            user.setEmail(email);
        }
        user.setNickname(realName);
        user.setUpdatedAt(LocalDateTime.now());
        if (!sysUserService.updateById(user)) {
            return R.fail("用户信息保存失败");
        }

        CandidateProfile profile = getOrCreateProfile(userId);
        profile.setRealName(realName);
        profile.setGender(request.getGender());
        profile.setBirthDate(parseDate(request.getBirthDate()));
        profile.setCurrentTitle(trim(request.getCurrentTitle()));
        profile.setWorkYears(request.getWorkYears());
        profile.setHighestEdu(highestEdu);
        profile.setCity(city);
        if (request.getJobSeekingStatus() != null) {
            profile.setJobSeekingStatus(request.getJobSeekingStatus());
        }
        profile.setUpdatedAt(LocalDateTime.now());

        CandidateProfileCompletenessVO completeness = evaluateCompleteness(user, profile);
        profile.setResumeCompleteness(completeness.getCompleteness());

        if (!candidateProfileService.updateById(profile)) {
            return R.fail("档案保存失败");
        }

        CandidateProfileVO vo = toVO(user, profile);
        vo.setProfileComplete(completeness.isComplete());
        vo.setResumeCompleteness(completeness.getCompleteness());
        return R.ok(vo, "保存成功");
    }

    private R<SysUser> requireCandidateUser(Long userId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() != 0)) {
            return R.fail("用户不存在");
        }
        if (user.getUserType() == null || user.getUserType() != USER_TYPE_CANDIDATE) {
            return R.fail("仅候选人可访问个人档案");
        }
        return R.ok(user);
    }

    private CandidateProfile getOrCreateProfile(Long userId) {
        LambdaQueryWrapper<CandidateProfile> w = new LambdaQueryWrapper<>();
        w.eq(CandidateProfile::getUserId, userId);
        CandidateProfile profile = candidateProfileService.getOne(w);
        if (profile == null) {
            profile = new CandidateProfile();
            profile.setUserId(userId);
            profile.setResumeCompleteness((byte) 0);
            profile.setIsDeleted(false);
            profile.setCreatedAt(LocalDateTime.now());
            profile.setUpdatedAt(LocalDateTime.now());
            candidateProfileService.save(profile);
        }
        return profile;
    }

    private CandidateProfileVO toVO(SysUser user, CandidateProfile profile) {
        CandidateProfileVO vo = new CandidateProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(user.getAvatarUrl());

        if (profile != null) {
            vo.setRealName(profile.getRealName());
            vo.setGender(profile.getGender());
            vo.setBirthDate(profile.getBirthDate() != null ? profile.getBirthDate().toString() : null);
            vo.setCurrentTitle(profile.getCurrentTitle());
            vo.setWorkYears(profile.getWorkYears());
            vo.setHighestEdu(profile.getHighestEdu());
            vo.setCity(profile.getCity());
            vo.setJobSeekingStatus(profile.getJobSeekingStatus());
            vo.setResumeCompleteness(profile.getResumeCompleteness());
            vo.setAiScore(profile.getAiScore());
        }
        return vo;
    }

    CandidateProfileCompletenessVO evaluateCompleteness(SysUser user, CandidateProfile profile) {
        List<String> missingRequired = new ArrayList<>();
        int filled = 0;
        int total = 5;

        if (profile != null && StringUtils.hasText(trim(profile.getRealName()))) {
            filled++;
        } else {
            missingRequired.add("真实姓名");
        }

        if (user != null && StringUtils.hasText(trim(user.getPhone()))
                && user.getPhone().matches("^1[3-9]\\d{9}$")) {
            filled++;
        } else {
            missingRequired.add("手机号");
        }

        if (profile != null && StringUtils.hasText(trim(profile.getCity()))) {
            filled++;
        } else {
            missingRequired.add("所在城市");
        }

        if (profile != null && profile.getHighestEdu() != null && profile.getHighestEdu() >= 1) {
            filled++;
        } else {
            missingRequired.add("最高学历");
        }

        if (profile != null && StringUtils.hasText(trim(profile.getCurrentTitle()))) {
            filled++;
        }

        CandidateProfileCompletenessVO vo = new CandidateProfileCompletenessVO();
        vo.setMissingFields(missingRequired);
        vo.setCompleteness((byte) Math.min(100, (filled * 100) / total));
        vo.setComplete(missingRequired.isEmpty());
        return vo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static LocalDate parseDate(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
