package com.network.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("repair_debug_detail")
public class RepairDebugDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;

    private String configurationChangeContent;

    private String oldFirmwareVersion;

    private String newFirmwareVersion;

    private String oldIpAddress;

    private String newIpAddress;

    private String oldGateway;

    private String newGateway;

    private String oldVlan;

    private String newVlan;

    private String routeChangeContent;

    private String firewallPolicyChange;

    private String permissionChangeContent;

    private String networkParameterChangeRecord;

    private String rollbackPlan;

    private String testResult;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
