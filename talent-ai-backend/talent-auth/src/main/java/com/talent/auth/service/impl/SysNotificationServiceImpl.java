package com.talent.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.auth.dto.NotificationBroadcastRequest;
import com.talent.auth.dto.NotificationCreateRequest;
import com.talent.auth.entity.SysNotification;
import com.talent.auth.entity.SysUser;
import com.talent.auth.mapper.SysNotificationMapper;
import com.talent.auth.service.ISysNotificationService;
import com.talent.auth.service.ISysUserService;
import com.talent.auth.vo.NotificationVO;
import com.talent.common.api.R;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysNotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification>
        implements ISysNotificationService {

    private final ISysUserService sysUserService;

    @Override
    public R<Map<String, Object>> listMyNotifications(Long userId, Integer current, Integer size) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        long pageNo = current != null && current > 0 ? current : 1;
        long pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;

        Page<SysNotification> page = page(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .orderByDesc(SysNotification::getCreatedAt));

        List<NotificationVO> records = page.getRecords().stream().map(this::toVO).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("pages", page.getPages());
        return R.ok(data);
    }

    @Override
    public R<Long> countUnread(Long userId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        Long count = count(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, false));
        return R.ok(count != null ? count : 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> markRead(Long userId, Long notificationId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        if (notificationId == null) {
            return R.fail("notificationId 不能为空");
        }
        SysNotification entity = getById(notificationId);
        if (entity == null || !userId.equals(entity.getUserId())) {
            return R.fail("通知不存在或无权操作");
        }
        if (Boolean.TRUE.equals(entity.getIsRead())) {
            return R.ok(null, "已读");
        }
        entity.setIsRead(true);
        entity.setUpdatedAt(LocalDateTime.now());
        updateById(entity);
        return R.ok(null, "已标记为已读");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> markAllRead(Long userId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        update(new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, false)
                .set(SysNotification::getIsRead, true)
                .set(SysNotification::getUpdatedAt, LocalDateTime.now()));
        return R.ok(null, "已全部标记为已读");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<NotificationVO> createNotification(NotificationCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            return R.fail("userId 不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            return R.fail("title 不能为空");
        }

        SysNotification entity = new SysNotification();
        entity.setUserId(request.getUserId());
        entity.setTitle(request.getTitle().trim());
        entity.setContent(StringUtils.hasText(request.getContent()) ? request.getContent().trim() : null);
        entity.setNotifyType(request.getNotifyType() != null ? request.getNotifyType() : (byte) 2);
        entity.setBizType(StringUtils.hasText(request.getBizType()) ? request.getBizType().trim() : null);
        entity.setBizId(request.getBizId());
        entity.setIsRead(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setIsDeleted(false);
        save(entity);
        return R.ok(toVO(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Map<String, Object>> broadcastAnnouncement(NotificationBroadcastRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            return R.fail("公告标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            return R.fail("公告内容不能为空");
        }

        List<SysUser> users = listUsersByTarget(request.getTarget());
        int pushed = 0;
        for (SysUser user : users) {
            NotificationCreateRequest create = new NotificationCreateRequest();
            create.setUserId(user.getId());
            create.setTitle(request.getTitle().trim());
            create.setContent(request.getContent().trim());
            create.setNotifyType((byte) 3);
            create.setBizType("announcement");
            create.setBizId(request.getAnnouncementId());
            R<NotificationVO> created = createNotification(create);
            if (created.getCode() == 200) {
                pushed++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pushedCount", pushed);
        data.put("targetUsers", users.size());
        return R.ok(data, "广播完成");
    }

    private List<SysUser> listUsersByTarget(String target) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getStatus, (byte) 1);
        if ("candidate".equalsIgnoreCase(target)) {
            w.eq(SysUser::getUserType, (byte) 1);
        } else if ("hr".equalsIgnoreCase(target)) {
            w.in(SysUser::getUserType, (byte) 2, (byte) 3, (byte) 4);
        }
        return sysUserService.list(w);
    }

    private NotificationVO toVO(SysNotification entity) {
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setNotifyType(entity.getNotifyType());
        vo.setNotifyTypeLabel(notifyTypeLabel(entity.getNotifyType()));
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setRead(Boolean.TRUE.equals(entity.getIsRead()));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private String notifyTypeLabel(Byte notifyType) {
        if (notifyType == null) {
            return "提醒";
        }
        return switch (notifyType) {
            case 1 -> "待办";
            case 3 -> "公告";
            default -> "提醒";
        };
    }
}
