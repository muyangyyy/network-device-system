package com.network.device.enums;

import lombok.Getter;

@Getter
public enum OrderPriority {

    LOW("低"),
    MEDIUM("中"),
    HIGH("高"),
    URGENT("紧急");

    private final String description;

    OrderPriority(String description) {
        this.description = description;
    }
}
