package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RepairOpticalDetailVO implements Serializable {

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
}
