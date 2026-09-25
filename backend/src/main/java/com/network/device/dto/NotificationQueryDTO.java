package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通知列表查询参数。
 *
 * <p>前端（api/notification.ts 的 getNotifications）传的是 {@code page} / {@code size}，
 * 与后端统一使用的 {@code pageSize} 不一致，此处用兼容 setter 转换，
 * 与 {@link UserQueryDTO} 的处理方式保持一致。
 */
@Data
public class NotificationQueryDTO implements Serializable {

    private Integer page = 1;

    private Integer pageSize = 10;

    /** 兼容前端 size 参数 */
    public void setSize(Integer size) {
        if (size != null) {
            this.pageSize = size;
        }
    }
}
