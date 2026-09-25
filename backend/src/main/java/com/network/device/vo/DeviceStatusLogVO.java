package com.network.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备状态变更日志视图对象。
 * 字段名与前端 api/device.ts 的 DeviceStatusLog 对齐：fromStatus / toStatus / reason / createTime。
 */
@Data
public class DeviceStatusLogVO implements Serializable {

    private Long id;

    private Long deviceId;

    private String deviceCode;

    /** 原状态（对应实体 originalStatus） */
    private String fromStatus;

    /** 新状态（对应实体 newStatus） */
    private String toStatus;

    private Long operatorId;

    private String operatorName;

    /** 变更原因（对应实体 remark） */
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
