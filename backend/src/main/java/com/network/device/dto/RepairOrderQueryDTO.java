package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RepairOrderQueryDTO implements Serializable {

    private String workOrderNo;

    private String deviceCode;

    private String deviceName;

    private String repairType;

    private String status;

    private Long repairUserId;

    private Long reporterId;

    private Long groupId;

    private String faultTimeStart;

    private String faultTimeEnd;

    private String priority;

    private Integer page = 1;

    private Integer pageSize = 10;

    /** 兼容前端传入的 size 参数 */
    public void setSize(Integer size) {
        if (size != null) {
            this.pageSize = size;
        }
    }
}
