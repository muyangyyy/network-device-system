package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeviceQueryDTO implements Serializable {

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private String brand;

    private String serialNumber;

    private String ipAddress;

    private String macAddress;

    private Long groupId;

    private Long responsibleUserId;

    private String status;

    private String installationLocation;

    private String purchaseDateStart;

    private String purchaseDateEnd;

    private String warrantyExpireDateStart;

    private String warrantyExpireDateEnd;

    private Integer page = 1;

    private Integer pageSize = 10;

    /** 兼容前端 size 参数 */
    public void setSize(Integer size) {
        if (size != null) {
            this.pageSize = size;
        }
    }
}
