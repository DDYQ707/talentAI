package com.talent.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.auth.config.OtpProperties;
import com.talent.auth.entity.AuthVerificationCode;
import com.talent.auth.entity.SysUser;
import com.talent.common.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LoginOtpService {

    private static final Logger log = LoggerFactory.getLogger(LoginOtpService.class);

    private static final byte CODE_TYPE_LOGIN = 1;

    private static final String REDIS_CODE_PREFIX = "auth:otp:login:";
    private static final String REDIS_COOLDOWN_PREFIX = "auth:otp:cooldown:";

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private IAuthVerificationCodeService authVerificationCodeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OtpProperties otpProperties;

    public Map<String, Object> sendLoginCode(String rawAccount) {
        Map<String, Object> result = new HashMap<>();
        String account = normalizeAccount(rawAccount);

        if (account.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "请输入手机号或邮箱");
            return result;
        }
        if (!isPhone(account) && !isEmail(account)) {
            result.put("code", 400);
            result.put("msg", isPhoneLikeInvalid(account)
                    ? "手机号格式不正确：需为大陆 11 位号段（1 开头，第 2 位为 3–9，如 13800138000）"
                    : "请输入正确的手机号（11 位大陆号段）或邮箱");
            return result;
        }

        SysUser user = findUserByAccount(account);
        if (user == null) {
            result.put("code", 404);
            result.put("msg", "账号不存在，请先注册或使用密码登录");
            return result;
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            result.put("code", 403);
            result.put("msg", "账号已被禁用，请联系管理员");
            return result;
        }

        if (isInCooldown(account)) {
            result.put("code", 429);
            result.put("msg", "发送过于频繁，请 " + otpProperties.getSendCooldownSeconds() + " 秒后再试");
            return result;
        }

        String code = generateCode();
        storeCode(account, code);
        markCooldown(account);
        persistCodeRecord(account, code);

        log.info("[OTP] 登录验证码 account={} code={}（实训环境未接入短信/邮件，请从日志或 devCode 获取）", account, code);

        result.put("code", 200);
        result.put("msg", "验证码已发送");
        result.put("expireSeconds", otpProperties.getExpireSeconds());
        if (otpProperties.isDevExpose()) {
            result.put("devCode", code);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> loginByCode(String rawAccount, String rawCode) {
        Map<String, Object> result = new HashMap<>();
        String account = normalizeAccount(rawAccount);
        String code = rawCode == null ? "" : rawCode.trim();

        if (account.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "请输入手机号或邮箱");
            return result;
        }
        if (!code.matches("^\\d{6}$")) {
            result.put("code", 400);
            result.put("msg", "请输入 6 位数字验证码");
            return result;
        }

        SysUser user = findUserByAccount(account);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "账号不存在");
            return result;
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            result.put("code", 403);
            result.put("msg", "账号已被禁用，请联系管理员");
            return result;
        }

        if (!verifyAndConsumeCode(account, code)) {
            result.put("code", 401);
            result.put("msg", "验证码错误或已过期");
            return result;
        }

        String roleStr = getRoleString(user.getUserType());
        String token = JwtUtil.generateToken(user.getId().toString(), roleStr);

        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("userType", user.getUserType());
        result.put("userInfo", userInfo);
        return result;
    }

    private void storeCode(String account, String code) {
        try {
            stringRedisTemplate.opsForValue().set(
                    REDIS_CODE_PREFIX + account,
                    code,
                    Duration.ofSeconds(otpProperties.getExpireSeconds())
            );
        } catch (Exception e) {
            log.warn("[OTP] Redis 写入失败，将仅依赖数据库校验: {}", e.getMessage());
        }
    }

    private void markCooldown(String account) {
        try {
            stringRedisTemplate.opsForValue().set(
                    REDIS_COOLDOWN_PREFIX + account,
                    "1",
                    Duration.ofSeconds(otpProperties.getSendCooldownSeconds())
            );
        } catch (Exception e) {
            log.warn("[OTP] Redis 冷却标记失败: {}", e.getMessage());
        }
    }

    private boolean isInCooldown(String account) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(REDIS_COOLDOWN_PREFIX + account));
        } catch (Exception e) {
            return hasRecentSendInDb(account);
        }
    }

    private boolean hasRecentSendInDb(String account) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(otpProperties.getSendCooldownSeconds());
        LambdaQueryWrapper<AuthVerificationCode> w = new LambdaQueryWrapper<>();
        w.eq(AuthVerificationCode::getAccount, account)
                .eq(AuthVerificationCode::getCodeType, CODE_TYPE_LOGIN)
                .ge(AuthVerificationCode::getCreatedAt, since);
        return authVerificationCodeService.count(w) > 0;
    }

    private void persistCodeRecord(String account, String code) {
        AuthVerificationCode record = new AuthVerificationCode();
        record.setAccount(account);
        record.setCode(code);
        record.setCodeType(CODE_TYPE_LOGIN);
        record.setExpireAt(LocalDateTime.now().plusSeconds(otpProperties.getExpireSeconds()));
        record.setIsUsed(false);
        authVerificationCodeService.save(record);
    }

    private boolean verifyAndConsumeCode(String account, String code) {
        String cached = null;
        try {
            cached = stringRedisTemplate.opsForValue().get(REDIS_CODE_PREFIX + account);
        } catch (Exception e) {
            log.warn("[OTP] Redis 读取失败，回退数据库: {}", e.getMessage());
        }

        if (cached != null && cached.equals(code)) {
            consumeRedisCode(account);
            markLatestDbCodeUsed(account, code);
            return true;
        }

        return verifyFromDb(account, code);
    }

    private void consumeRedisCode(String account) {
        try {
            stringRedisTemplate.delete(REDIS_CODE_PREFIX + account);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private boolean verifyFromDb(String account, String code) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<AuthVerificationCode> w = new LambdaQueryWrapper<>();
        w.eq(AuthVerificationCode::getAccount, account)
                .eq(AuthVerificationCode::getCodeType, CODE_TYPE_LOGIN)
                .eq(AuthVerificationCode::getCode, code)
                .eq(AuthVerificationCode::getIsUsed, false)
                .ge(AuthVerificationCode::getExpireAt, now)
                .orderByDesc(AuthVerificationCode::getId)
                .last("LIMIT 1");

        AuthVerificationCode record = authVerificationCodeService.getOne(w);
        if (record == null) {
            return false;
        }
        record.setIsUsed(true);
        authVerificationCodeService.updateById(record);
        consumeRedisCode(account);
        return true;
    }

    private void markLatestDbCodeUsed(String account, String code) {
        LambdaQueryWrapper<AuthVerificationCode> w = new LambdaQueryWrapper<>();
        w.eq(AuthVerificationCode::getAccount, account)
                .eq(AuthVerificationCode::getCodeType, CODE_TYPE_LOGIN)
                .eq(AuthVerificationCode::getCode, code)
                .eq(AuthVerificationCode::getIsUsed, false)
                .orderByDesc(AuthVerificationCode::getId)
                .last("LIMIT 1");
        AuthVerificationCode record = authVerificationCodeService.getOne(w);
        if (record != null) {
            record.setIsUsed(true);
            authVerificationCodeService.updateById(record);
        }
    }

    private SysUser findUserByAccount(String account) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.and(q -> q.eq(SysUser::getUsername, account)
                .or().eq(SysUser::getPhone, account)
                .or().eq(SysUser::getEmail, account));
        return sysUserService.getOne(w);
    }

    private String generateCode() {
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private String normalizeAccount(String account) {
        return account == null ? "" : account.trim();
    }

    private boolean isPhone(String value) {
        return value.matches("^1[3-9]\\d{9}$");
    }

    private boolean isEmail(String value) {
        return value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean isPhoneLikeInvalid(String value) {
        return value.matches("^1\\d{10}$") && !isPhone(value);
    }

    private String getRoleString(Byte userType) {
        if (userType == null) {
            return "UNKNOWN";
        }
        switch (userType) {
            case 1:
                return "CANDIDATE";
            case 2:
                return "HR";
            case 3:
                return "INTERVIEWER";
            case 4:
                return "ADMIN";
            default:
                return "UNKNOWN";
        }
    }
}
