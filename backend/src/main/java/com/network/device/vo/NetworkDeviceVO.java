package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class NetworkDeviceVO implements Serializable {

    private Long id;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private String brand;

    private String model;

    private String serialNumber;

    private LocalDate purchaseDate;

    private LocalDate warrantyExpireDate;

    private String installationLocation;

    private String ipAddress;

    private String macAddress;

    private String status;

    private String statusName;

    private Long groupId;

    private String groupName;

    private Long responsibleUserId;

    private String responsibleUserName;

    private String supplier;

    private String department;

    private String remark;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer totalRepairs;

    private Integer completedRepairs;
}
