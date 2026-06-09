package com.talent.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.auth.entity.SysUser;
import com.talent.auth.vo.InterviewerOptionVO;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HrStaffService {

    private static final byte TYPE_INTERVIEWER = 3;
    private static final byte STATUS_NORMAL = 1;

    private final ISysUserService sysUserService;

    public List<InterviewerOptionVO> listInterviewers(String role) {
        assertHrOrAdmin(role);
        List<SysUser> users = sysUserService.list(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserType, TYPE_INTERVIEWER)
                        .eq(SysUser::getStatus, STATUS_NORMAL)
                        .orderByAsc(SysUser::getNickname));
        return users.stream().map(this::toVo).collect(Collectors.toList());
    }

    private InterviewerOptionVO toVo(SysUser user) {
        InterviewerOptionVO vo = new InterviewerOptionVO();
        vo.setId(user.getId());
        vo.setAccount(user.getUsername());
        String nickname = user.getNickname();
        vo.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : user.getUsername());
        return vo;
    }

    private void assertHrOrAdmin(String role) {
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("仅 HR 或管理员可访问");
        }
        String r = role.trim().toUpperCase();
        if (!"HR".equals(r) && !"ADMIN".equals(r)) {
            throw new IllegalArgumentException("仅 HR 或管理员可访问");
        }
    }
}
