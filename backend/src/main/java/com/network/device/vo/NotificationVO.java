package com.network.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息视图对象。
 * 字段名与前端 api/notification.ts 的 Notification 对齐：isRead / createTime。
 */
@Data
public class NotificationVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String type;

    /** 是否已读（对应实体 readStatus：1-已读 0-未读） */
    private Boolean isRead;

    private Long relatedId;

    private String relatedType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
