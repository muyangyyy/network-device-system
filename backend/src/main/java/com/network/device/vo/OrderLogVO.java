package com.network.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单状态流转日志视图对象。
 * 字段名与前端 api/repair.ts 的 OrderLog 对齐：orderNo / fromStatus / toStatus / createTime。
 */
@Data
public class OrderLogVO implements Serializable {

    private Long id;

    /** 工单编号（对应实体 workOrderNo） */
    private String orderNo;

    /** 原状态（对应实体 originalStatus） */
    private String fromStatus;

    /** 新状态（对应实体 newStatus） */
    private String toStatus;

    private Long operatorId;

    private String operatorName;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
