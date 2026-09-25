package com.network.device.enums;

import lombok.Getter;

@Getter
public enum RepairStatus {

    DRAFT("草稿"),
    SUBMITTED("已提交"),
    ASSIGNED("已分配"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    ACCEPTED("已验收"),
    REJECTED("已退回"),
    DELAYED("已延期"),
    REPAIR_AGAIN("返修"),
    CLOSED("已关闭"),
    CANCELLED("已取消");

    private final String description;

    RepairStatus(String description) {
        this.description = description;
    }
}
