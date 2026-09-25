package com.network.device.controller;

import com.network.device.common.PageResult;
import com.network.device.common.Result;
import com.network.device.dto.NotificationQueryDTO;
import com.network.device.security.LoginUser;
import com.network.device.service.NotificationService;
import com.network.device.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知管理", description = "通知消息管理接口")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "分页获取当前用户通知列表")
    @GetMapping
    public Result<PageResult<NotificationVO>> listNotifications(NotificationQueryDTO queryDTO) {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.getUserNotifications(userId, queryDTO));
    }

    @Operation(summary = "获取未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> countUnread() {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.countUnread(userId));
    }

    @Operation(summary = "标记通知为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, getCurrentUserId());
        return Result.success("已标记为已读");
    }

    @Operation(summary = "标记所有通知为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("已全部标记为已读");
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return ((LoginUser) authentication.getPrincipal()).getId();
        }
        return null;
    }
}
