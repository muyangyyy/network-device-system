package com.network.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("repair_hardware_detail")
public class RepairHardwareDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;

    private String damagedComponent;

    private String replacementPartName;

    private String replacementPartModel;

    private Integer replacementPartQuantity;

    private BigDecimal replacementPartCost;

    private String oldPartDisposalMethod;

    private String hardwareFailureCode;

    private Integer whetherUnderWarranty;

    private String supplier;

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
