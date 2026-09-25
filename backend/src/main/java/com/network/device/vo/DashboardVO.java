package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO implements Serializable {

    private Long totalDevices;

    private Long normalDevices;

    private Long faultDevices;

    private Long idleDevices;

    private Long scrappedDevices;

    private Long pendingOrders;

    private Long monthlyOrders;

    /** 平均维修时长（小时） */
    private Double avgRepairTime;

    /** 完成率（%） */
    private Double completionRate;

    /** 验收通过率（%） */
    private Double acceptanceRate;

    private Long overdueOrders;

    private Map<String, Long> deviceTypeDistribution;

    private Map<String, Long> repairTypeDistribution;

    private List<RepairWorkOrderVO> recentOrders;

    private List<DeviceStatusLogVO> recentStatusChanges;

    private List<RepairWorkOrderVO> pendingAcceptanceOrders;
}
