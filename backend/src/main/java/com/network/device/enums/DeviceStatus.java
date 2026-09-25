package com.network.device.enums;

import lombok.Getter;

@Getter
public enum DeviceStatus {

    NORMAL("正常运行"),
    FAULT_REPAIR("故障维修"),
    IDLE("闲置"),
    SCRAPPED("报废");

    private final String description;

    DeviceStatus(String description) {
        this.description = description;
    }
}
