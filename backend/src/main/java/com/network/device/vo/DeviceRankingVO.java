package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

/** 设备维修次数排名项 */
@Data
public class DeviceRankingVO implements Serializable {

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Long repairCount;
}
