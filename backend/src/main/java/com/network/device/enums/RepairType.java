package com.network.device.enums;

import lombok.Getter;

@Getter
public enum RepairType {

    HARDWARE("硬件维修"),
    DEBUG("调试维修"),
    OPTICAL("光路异常维修");

    private final String description;

    RepairType(String description) {
        this.description = description;
    }
}
