package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class StatisticsQueryDTO implements Serializable {

    private String startTime;

    private String endTime;

    private Long groupId;

    private String deviceType;

    private String repairType;

    private Long repairUserId;

    private String deviceStatus;

    /** 趋势粒度：daily / weekly / monthly */
    private String period = "daily";
}
