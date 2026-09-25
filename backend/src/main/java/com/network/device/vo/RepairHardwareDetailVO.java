package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RepairHardwareDetailVO implements Serializable {

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
}
