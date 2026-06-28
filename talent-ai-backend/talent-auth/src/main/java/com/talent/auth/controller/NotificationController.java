package com.talent.auth.controller;

import com.talent.auth.dto.NotificationCreateRequest;
import com.talent.auth.service.ISysNotificationService;
import com.talent.auth.vo.NotificationVO;
import com.talent.common.api.R;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final ISysNotificationService notificationService;

    @GetMapping("/my")
    public R<Map<String, Object>> myNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return notificationService.listMyNotifications(userId, current, size);
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return notificationService.countUnread(userId);
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") Long id) {
        return notificationService.markRead(userId, id);
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return notificationService.markAllRead(userId);
    }

    /** 微服务内部：创建通知 */
    @PostMapping("/internal/create")
    public Map<String, Object> createInternal(@RequestBody NotificationCreateRequest request) {
        R<NotificationVO> result = notificationService.createNotification(request);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("code", result.getCode());
        body.put("msg", result.getMsg());
        if (result.getData() != null) {
            body.put("data", result.getData());
        }
        return body;
    }

    /** 微服务内部：公告广播推送 */
    @PostMapping("/internal/broadcast")
    public Map<String, Object> broadcastInternal(@RequestBody com.talent.auth.dto.NotificationBroadcastRequest request) {
        R<Map<String, Object>> result = notificationService.broadcastAnnouncement(request);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("code", result.getCode());
        body.put("msg", result.getMsg());
        if (result.getData() != null) {
            body.put("data", result.getData());
        }
        return body;
    }
}
