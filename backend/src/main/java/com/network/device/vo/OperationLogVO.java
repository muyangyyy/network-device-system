package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志 VO —— 字段与前端 系统日志页 保持一致。
 */
@Data
public class OperationLogVO implements Serializable {

    private Long id;

    /** 操作人 */
    private String operatorName;

    /** 操作类型：LOGIN / LOGOUT / CREATE / UPDATE / DELETE / OTHER */
    private String operationType;

    /** 操作模块 */
    private String module;

    /** 操作描述 */
    private String description;

    /** 操作人IP */
    private String ip;

    /** 操作结果：1-成功 0-失败 */
    private Integer status;

    /** 操作时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
