package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统日志查询参数。
 * 前端分页参数使用 size，后端统一使用 pageSize，此处做兼容。
 */
@Data
public class LogQueryDTO implements Serializable {

    private String operatorName;

    /** LOGIN / LOGOUT / CREATE / UPDATE / DELETE */
    private String operationType;

    private String module;

    /** yyyy-MM-dd */
    private String startTime;

    /** yyyy-MM-dd */
    private String endTime;

    private Integer page = 1;

    private Integer pageSize = 10;

    /** 兼容前端 size 参数 */
    public void setSize(Integer size) {
        if (size != null) {
            this.pageSize = size;
        }
    }
}
