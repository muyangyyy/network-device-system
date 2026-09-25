package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CompleteDTO implements Serializable {

    private String repairSolution;

    private String repairResult;

    private String remark;

    private HardwareDetailDTO hardwareDetail;

    private DebugDetailDTO debugDetail;

    private OpticalDetailDTO opticalDetail;

    @Data
    public static class HardwareDetailDTO implements Serializable {
        private String damagedComponent;
        private String replacementPartName;
        private String replacementPartModel;
        private Integer replacementPartQuantity;
        private java.math.BigDecimal replacementPartCost;
        private String oldPartDisposalMethod;
        private String hardwareFailureCode;
        private Integer whetherUnderWarranty;
        private String supplier;
        private String remark;
    }

    @Data
    public static class DebugDetailDTO implements Serializable {
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
    }

    @Data
    public static class OpticalDetailDTO implements Serializable {
        private String faultOpticalPoint;
        private String opticalRouteName;
        private String cableSection;
        private java.math.BigDecimal cableLength;
        private java.math.BigDecimal opticalPowerBefore;
        private java.math.BigDecimal opticalPowerAfter;
        private java.math.BigDecimal attenuationBefore;
        private java.math.BigDecimal attenuationAfter;
        private String wavelength;
        private String splitterStatus;
        private String jumperStatus;
        private String cableDamageDescription;
        private String linkTestResult;
        private String testTool;
        private String testPerson;
        private String remark;
    }
}
