package com.talent.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.auth.entity.CandidateProfile;
import com.talent.auth.entity.SysUser;
import com.talent.auth.dto.AdminAccountSaveRequest;
import com.talent.auth.dto.ParseProfilePatchRequest;
import com.talent.auth.service.AdminAccountService;
import com.talent.auth.service.CandidateMyProfileService;
import com.talent.auth.service.ICandidateProfileService;
import com.talent.auth.service.ISysUserService;
import com.talent.auth.service.LoginOtpService;
import com.talent.common.utils.JwtUtil; // 如果你把它移到了 common，注意改一下导入路径
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ICandidateProfileService candidateProfileService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private CandidateMyProfileService candidateMyProfileService;

    @Autowired
    private LoginOtpService loginOtpService;

    /** 发送登录验证码（手机号/邮箱，实训环境写日志，不实际发短信/邮件） */
    @PostMapping("/otp/send")
    public Map<String, Object> sendLoginOtp(@RequestParam("account") String account) {
        return loginOtpService.sendLoginCode(account);
    }

    /** 验证码登录 */
    @PostMapping("/login/otp")
    public Map<String, Object> loginByOtp(@RequestParam("account") String account,
                                          @RequestParam("code") String code) {
        return loginOtpService.loginByCode(account, code);
    }

    @GetMapping("/getUserName")
    public String getUserName(@RequestParam("id") Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return "未知用户";
        }
        LambdaQueryWrapper<CandidateProfile> w = new LambdaQueryWrapper<>();
        w.eq(CandidateProfile::getUserId, id);
        CandidateProfile profile = candidateProfileService.getOne(w);
        if (profile != null && profile.getRealName() != null && !profile.getRealName().isBlank()) {
            return profile.getRealName().trim();
        }
        return user.getNickname() != null ? user.getNickname() : "未知用户";
    }

    /** 微服务内部：候选人档案摘要（与 getUserName 同路径风格，供 Feign 调用） */
    @GetMapping("/internal/candidateBrief")
    public Map<String, Object> internalCandidateBrief(@RequestParam("userId") Long userId) {
        return candidateMyProfileService.getProfileBrief(userId);
    }

    /** 微服务内部：AI 解析结果回填候选人档案（仅补空字段） */
    @PostMapping("/internal/patch-from-parse")
    public Map<String, Object> patchProfileFromParse(@RequestBody ParseProfilePatchRequest request) {
        return candidateMyProfileService.patchFromParse(request);
    }

    /** 微服务内部：用户简要信息（安排面试、查昵称） */
    @GetMapping("/internal/user/brief")
    public Map<String, Object> internalUserBrief(@RequestParam("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("code", 400);
            result.put("msg", "userId 不能为空");
            return result;
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            result.put("code", 404);
            result.put("msg", "用户不存在");
            return result;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("nickname", resolveDisplayName(user));
        data.put("userType", user.getUserType());
        data.put("account", user.getUsername());
        result.put("code", 200);
        result.put("msg", "ok");
        result.put("data", data);
        return result;
    }

    private String resolveDisplayName(SysUser user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return "用户";
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam("username") String username,
                                     @RequestParam("password") String password) {
        Map<String, Object> result = new HashMap<>();

        // 1. 支持用户名 / 手机号 / 邮箱登录
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(w -> w.eq(SysUser::getUsername, username)
                .or().eq(SysUser::getPhone, username)
                .or().eq(SysUser::getEmail, username));
        SysUser user = sysUserService.getOne(queryWrapper);

        // 2. 如果查不到用户，说明账号不存在
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "账号不存在！");
            return result;
        }

        // 3. 校验密码：用明文 password 和数据库里的密文 passwordHash 进行对比
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            result.put("code", 401);
            result.put("msg", "密码错误！");
            return result;
        }

        // 4. 校验账号状态是否被禁用 (1: 正常)
        if (user.getStatus() == null || user.getStatus() != 1) {
            result.put("code", 403);
            result.put("msg", "账号已被禁用，请联系管理员！");
            return result;
        }

        // 5. 校验通过，生成真正的业务 Token！
        // 将 userType 映射为字符串角色 (1-候选人 2-HR 3-面试官 4-管理员)
        String roleStr = getRoleString(user.getUserType());
        // 将用户真实的 ID 和 角色 写入 JWT
        String token = JwtUtil.generateToken(user.getId().toString(), roleStr);

        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("token", token);

        // 通常登录成功还会把用户基本信息返回给前端，方便前端展示头像昵称等
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("userType", user.getUserType());
        result.put("userInfo", userInfo);

        return result;
    }

    /** 求职者自助注册（固定 userType=1，不接受其它类型） */
    @PostMapping("/register")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(@RequestParam("account") String account,
                                        @RequestParam("password") String password) {
        Map<String, Object> result = new HashMap<>();
        String acct = account == null ? "" : account.trim();

        if (acct.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "请输入手机号或邮箱");
            return result;
        }
        if (password == null || password.length() < 6) {
            result.put("code", 400);
            result.put("msg", "密码至少 6 位");
            return result;
        }
        if (!isPhone(acct) && !isEmail(acct)) {
            result.put("code", 400);
            result.put("msg", acct.matches("^1\\d{10}$")
                    ? "手机号格式不正确：需为大陆 11 位号段（1 开头，第 2 位为 3–9，如 13800138000）"
                    : "请输入正确的手机号（11 位大陆号段）或邮箱");
            return result;
        }

        if (existsAccount(acct)) {
            result.put("code", 409);
            result.put("msg", conflictMessage(acct));
            return result;
        }

        SysUser user = new SysUser();
        user.setUsername(acct);
        if (isPhone(acct)) {
            user.setPhone(acct);
        } else {
            user.setEmail(acct);
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(acct);
        user.setUserType((byte) 1);
        user.setStatus((byte) 1);
        user.setIsDeleted(0);

        if (!sysUserService.save(user)) {
            result.put("code", 500);
            result.put("msg", "注册失败，请稍后重试");
            return result;
        }

        CandidateProfile profile = new CandidateProfile();
        profile.setUserId(user.getId());
        profile.setResumeCompleteness((byte) 0);
        profile.setIsDeleted(false);
        if (!candidateProfileService.save(profile)) {
            throw new IllegalStateException("候选人档案创建失败");
        }

        result.put("code", 200);
        result.put("msg", "注册成功");
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("userType", user.getUserType());
        result.put("userInfo", userInfo);
        return result;
    }

    private boolean isPhone(String value) {
        return value.matches("^1[3-9]\\d{9}$");
    }

    private boolean isEmail(String value) {
        return value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean existsAccount(String acct) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.and(q -> q.eq(SysUser::getUsername, acct)
                .or().eq(SysUser::getPhone, acct)
                .or().eq(SysUser::getEmail, acct));
        return sysUserService.count(w) > 0;
    }

    private String conflictMessage(String acct) {
        if (isPhone(acct)) {
            return "该手机号已被注册";
        }
        if (isEmail(acct)) {
            return "该邮箱已被注册";
        }
        return "该账号已被注册";
    }

    // ---------- 管理员：HR / 面试官账号 CRUD ----------

    @GetMapping("/admin/accounts")
    public Map<String, Object> adminListAccounts(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "userType", required = false) Byte userType) {
        return adminAccountService.list(role, page, size, keyword, userType);
    }

    @PostMapping("/admin/accounts")
    public Map<String, Object> adminCreateAccount(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody AdminAccountSaveRequest body) {
        return adminAccountService.create(role, body);
    }

    @PutMapping("/admin/accounts/{id}")
    public Map<String, Object> adminUpdateAccount(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("id") Long id,
            @RequestBody AdminAccountSaveRequest body) {
        return adminAccountService.update(role, id, body);
    }

    @DeleteMapping("/admin/accounts/{id}")
    public Map<String, Object> adminDeleteAccount(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("id") Long id) {
        return adminAccountService.delete(role, id);
    }

    /**
     * 辅助方法：将数据库里的 userType 数字转成大写的角色标识
     */
    private String getRoleString(Byte userType) {
        if (userType == null) return "UNKNOWN";
        switch (userType) {
            case 1: return "CANDIDATE";
            case 2: return "HR";
            case 3: return "INTERVIEWER";
            case 4: return "ADMIN";
            default: return "UNKNOWN";
        }
    }
}