package com.talent.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.talent.auth.dto.AdminAccountSaveRequest;
import com.talent.auth.entity.SysUser;
import com.talent.auth.vo.AdminAccountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminAccountService {

    private static final Byte TYPE_HR = 2;
    private static final Byte TYPE_INTERVIEWER = 3;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> list(String role, long page, long size, String keyword, Byte userType) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(role)) {
            return forbidden(result);
        }
        if (userType != null && !isManagedType(userType)) {
            result.put("code", 400);
            result.put("msg", "仅支持筛选 HR 或面试官账号");
            return result;
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getUserType, 2, 3);
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getPhone, kw)
                    .or().like(SysUser::getEmail, kw)
                    .or().like(SysUser::getNickname, kw));
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> pageResult = sysUserService.page(new Page<>(page, size), wrapper);
        List<AdminAccountVO> records = pageResult.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", pageResult.getTotal());
        data.put("page", pageResult.getCurrent());
        data.put("size", pageResult.getSize());

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    public Map<String, Object> create(String role, AdminAccountSaveRequest body) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(role)) {
            return forbidden(result);
        }

        String validateMsg = validateForCreate(body);
        if (validateMsg != null) {
            result.put("code", 400);
            result.put("msg", validateMsg);
            return result;
        }

        String acct = body.getAccount().trim();
        if (existsConflict(acct, null)) {
            result.put("code", 409);
            result.put("msg", "该账号已被使用");
            return result;
        }

        SysUser user = buildUserFromRequest(body, acct);
        if (!sysUserService.save(user)) {
            result.put("code", 500);
            result.put("msg", "创建失败");
            return result;
        }

        result.put("code", 200);
        result.put("msg", "创建成功");
        result.put("data", toVo(user));
        return result;
    }

    public Map<String, Object> update(String role, Long id, AdminAccountSaveRequest body) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(role)) {
            return forbidden(result);
        }

        SysUser existing = sysUserService.getById(id);
        if (existing == null || !isManagedType(existing.getUserType())) {
            result.put("code", 404);
            result.put("msg", "账号不存在");
            return result;
        }

        String validateMsg = validateForUpdate(body);
        if (validateMsg != null) {
            result.put("code", 400);
            result.put("msg", validateMsg);
            return result;
        }

        String acct = body.getAccount().trim();
        if (existsConflict(acct, id)) {
            result.put("code", 409);
            result.put("msg", "该账号已被使用");
            return result;
        }

        applyAccountFields(existing, acct);
        existing.setUserType(body.getUserType());
        existing.setNickname(StringUtils.hasText(body.getNickname()) ? body.getNickname().trim() : acct);
        if (body.getStatus() != null) {
            existing.setStatus(body.getStatus());
        }
        if (StringUtils.hasText(body.getPassword())) {
            if (body.getPassword().length() < 6) {
                result.put("code", 400);
                result.put("msg", "密码至少 6 位");
                return result;
            }
            existing.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        }

        if (!sysUserService.updateById(existing)) {
            result.put("code", 500);
            result.put("msg", "更新失败");
            return result;
        }

        result.put("code", 200);
        result.put("msg", "更新成功");
        result.put("data", toVo(existing));
        return result;
    }

    public Map<String, Object> delete(String role, Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(role)) {
            return forbidden(result);
        }

        SysUser existing = sysUserService.getById(id);
        if (existing == null || !isManagedType(existing.getUserType())) {
            result.put("code", 404);
            result.put("msg", "账号不存在");
            return result;
        }

        if (!sysUserService.removeById(id)) {
            result.put("code", 500);
            result.put("msg", "删除失败");
            return result;
        }

        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    private boolean isManagedType(Byte userType) {
        return TYPE_HR.equals(userType) || TYPE_INTERVIEWER.equals(userType);
    }

    private Map<String, Object> forbidden(Map<String, Object> result) {
        result.put("code", 403);
        result.put("msg", "仅系统管理员可管理企业账号");
        return result;
    }

    private String validateForCreate(AdminAccountSaveRequest body) {
        if (body == null) {
            return "请求体不能为空";
        }
        if (!StringUtils.hasText(body.getAccount())) {
            return "请输入手机号或邮箱";
        }
        if (!isPhone(body.getAccount().trim()) && !isEmail(body.getAccount().trim())) {
            return "请输入正确的手机号或邮箱";
        }
        if (!StringUtils.hasText(body.getPassword()) || body.getPassword().length() < 6) {
            return "初始密码至少 6 位";
        }
        if (!isManagedType(body.getUserType())) {
            return "账号类型须为 HR 或面试官";
        }
        return null;
    }

    private String validateForUpdate(AdminAccountSaveRequest body) {
        if (body == null) {
            return "请求体不能为空";
        }
        if (!StringUtils.hasText(body.getAccount())) {
            return "请输入手机号或邮箱";
        }
        if (!isPhone(body.getAccount().trim()) && !isEmail(body.getAccount().trim())) {
            return "请输入正确的手机号或邮箱";
        }
        if (!isManagedType(body.getUserType())) {
            return "账号类型须为 HR 或面试官";
        }
        if (StringUtils.hasText(body.getPassword()) && body.getPassword().length() < 6) {
            return "密码至少 6 位";
        }
        if (body.getStatus() != null && body.getStatus() != 0 && body.getStatus() != 1) {
            return "状态仅支持启用或禁用";
        }
        return null;
    }

    private SysUser buildUserFromRequest(AdminAccountSaveRequest body, String acct) {
        SysUser user = new SysUser();
        applyAccountFields(user, acct);
        user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        user.setUserType(body.getUserType());
        user.setNickname(StringUtils.hasText(body.getNickname()) ? body.getNickname().trim() : acct);
        user.setStatus((byte) 1);
        user.setIsDeleted(0);
        return user;
    }

    private void applyAccountFields(SysUser user, String acct) {
        user.setUsername(acct);
        user.setPhone(null);
        user.setEmail(null);
        if (isPhone(acct)) {
            user.setPhone(acct);
        } else {
            user.setEmail(acct);
        }
    }

    private boolean existsConflict(String acct, Long excludeId) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.and(q -> q.eq(SysUser::getUsername, acct)
                .or().eq(SysUser::getPhone, acct)
                .or().eq(SysUser::getEmail, acct));
        if (excludeId != null) {
            w.ne(SysUser::getId, excludeId);
        }
        return sysUserService.count(w) > 0;
    }

    private AdminAccountVO toVo(SysUser user) {
        AdminAccountVO vo = new AdminAccountVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAccount(resolveAccount(user));
        vo.setNickname(user.getNickname());
        vo.setUserType(user.getUserType());
        vo.setUserTypeLabel(userTypeLabel(user.getUserType()));
        vo.setStatus(user.getStatus());
        vo.setStatusLabel(statusLabel(user.getStatus()));
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }

    private String resolveAccount(SysUser user) {
        if (StringUtils.hasText(user.getPhone())) {
            return user.getPhone();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return user.getUsername();
    }

    private String userTypeLabel(Byte userType) {
        if (TYPE_HR.equals(userType)) {
            return "HR";
        }
        if (TYPE_INTERVIEWER.equals(userType)) {
            return "面试官";
        }
        return "未知";
    }

    private String statusLabel(Byte status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "正常" : "禁用";
    }

    private boolean isPhone(String value) {
        return value.matches("^1[3-9]\\d{9}$");
    }

    private boolean isEmail(String value) {
        return value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
