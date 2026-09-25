package com.network.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("notification_message")
public class NotificationMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Long senderId;

    private Long receiverId;

    private String type;

    private Long relatedId;

    private String relatedType;

    private Integer readStatus;

    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
