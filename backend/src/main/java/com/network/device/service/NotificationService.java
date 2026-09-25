package com.network.device.service;

import com.network.device.common.PageResult;
import com.network.device.dto.NotificationQueryDTO;
import com.network.device.vo.NotificationVO;

public interface NotificationService {

    void sendNotification(Long receiverId, String title, String content, String type, Long relatedId, String relatedType);

    /**
     * 分页查询某用户的通知列表。
     *
     * <p>返回 {@link PageResult}（含 records / total / page / pageSize）而不是裸 List：
     * 前端 store/notification.ts 是按 {@code res.records} / {@code res.total} 消费的，
     * 返回数组会让 {@code notifications} 变成 undefined，进而使未读数计算属性报错、整个顶栏渲染失败。
     */
    PageResult<NotificationVO> getUserNotifications(Long userId, NotificationQueryDTO query);

    /**
     * 未读通知数。
     *
     * <p>前端顶栏徽标需要的是「真实未读总数」，而 store 目前是在已拉取的首页数据里 filter 出来的，
     * 未读数超过一页时会被截断。这里提供独立的计数接口。
     */
    Long countUnread(Long userId);

    /**
     * 标记单条通知为已读。
     *
     * <p>必须带 {@code userId} 做归属校验：否则任意登录用户只要遍历 id 就能把别人的通知
     * 标记成已读（越权写入）。不属于该用户时静默忽略，不泄露「该 id 是否存在」。
     */
    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);
}
