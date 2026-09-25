package com.network.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("repair_optical_detail")
public class RepairOpticalDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;

    private String faultOpticalPoint;

    private String opticalRouteName;

    private String cableSection;

    private BigDecimal cableLength;

    private BigDecimal opticalPowerBefore;

    private BigDecimal opticalPowerAfter;

    private BigDecimal attenuationBefore;

    private BigDecimal attenuationAfter;

    private String wavelength;

    private String splitterStatus;

    private String jumperStatus;

    private String cableDamageDescription;

    private String linkTestResult;

    private String testTool;

    private String testPerson;

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
