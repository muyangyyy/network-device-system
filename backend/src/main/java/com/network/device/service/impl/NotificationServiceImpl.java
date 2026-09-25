package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.common.PageResult;
import com.network.device.dto.NotificationQueryDTO;
import com.network.device.entity.NotificationMessage;
import com.network.device.mapper.NotificationMessageMapper;
import com.network.device.service.NotificationService;
import com.network.device.vo.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMessageMapper notificationMessageMapper;

    public NotificationServiceImpl(NotificationMessageMapper notificationMessageMapper) {
        this.notificationMessageMapper = notificationMessageMapper;
    }

    @Override
    @Transactional
    public void sendNotification(Long receiverId, String title, String content, String type, Long relatedId, String relatedType) {
        NotificationMessage notification = new NotificationMessage();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setReadStatus(0);
        notificationMessageMapper.insert(notification);
    }

    @Override
    public PageResult<NotificationVO> getUserNotifications(Long userId, NotificationQueryDTO query) {
        int pageNo = query == null || query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1
                ? 10 : query.getPageSize();

        IPage<NotificationMessage> page = notificationMessageMapper.selectPageByReceiverId(
                new Page<>(pageNo, pageSize), userId);

        List<NotificationVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public Long countUnread(Long userId) {
        Long count = notificationMessageMapper.countUnread(userId);
        return count == null ? 0L : count;
    }

    @Override
    @Transactional
    public void markAsRead(Long id, Long userId) {
        NotificationMessage notification = notificationMessageMapper.selectById(id);
        // 归属校验：只允许本人操作自己的通知，避免越权把他人通知标记为已读。
        if (notification == null || userId == null || !userId.equals(notification.getReceiverId())) {
            return;
        }
        if (notification.getReadStatus() != null && notification.getReadStatus() == 1) {
            return;
        }
        notification.setReadStatus(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMessageMapper.updateById(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMessageMapper.markAllAsRead(userId);
    }

    private NotificationVO toVO(NotificationMessage message) {
        NotificationVO vo = new NotificationVO();
        vo.setId(message.getId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setType(message.getType());
        vo.setIsRead(message.getReadStatus() != null && message.getReadStatus() == 1);
        vo.setRelatedId(message.getRelatedId());
        vo.setRelatedType(message.getRelatedType());
        vo.setCreateTime(message.getCreatedAt());
        return vo;
    }
}
