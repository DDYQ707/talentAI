package com.talent.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.talent.auth.dto.NotificationBroadcastRequest;
import com.talent.auth.dto.NotificationCreateRequest;
import com.talent.auth.entity.SysNotification;
import com.talent.auth.vo.NotificationVO;
import com.talent.common.api.R;
import java.util.Map;

public interface ISysNotificationService extends IService<SysNotification> {

    R<Map<String, Object>> listMyNotifications(Long userId, Integer current, Integer size);

    R<Long> countUnread(Long userId);

    R<Void> markRead(Long userId, Long notificationId);

    R<Void> markAllRead(Long userId);

    R<NotificationVO> createNotification(NotificationCreateRequest request);

    /** 按目标人群批量推送公告通知 */
    R<Map<String, Object>> broadcastAnnouncement(NotificationBroadcastRequest request);
}
