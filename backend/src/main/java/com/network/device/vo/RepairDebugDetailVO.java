package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class RepairDebugDetailVO implements Serializable {

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
}
