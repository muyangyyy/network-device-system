package com.network.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class NetworkDeviceDTO implements Serializable {

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    private String deviceType;

    /**
     * 初始状态，仅新增时生效。
     *
     * <p>取值必须限定为 {@code NORMAL / FAULT_REPAIR / IDLE / SCRAPPED}——
     * 这四个是 {@code DeviceStatus} 枚举、{@code device_status_log}、
     * 前端 DeviceStatusTag 与列表筛选器共同认可的取值。前端表单曾经提供
     * {@code FAULT / MAINTENANCE} 两个库里不存在的状态，一旦落库，
     * 标签会退化成裸英文、筛选器永远匹配不到，因此这里用 @Pattern 兜住。
     *
     * <p>修改状态请走 {@code PUT /api/devices/{id}/status}：只有那条路径会写
     * {@code device_status_log} 变更日志。
     */
    @Pattern(regexp = "NORMAL|FAULT_REPAIR|IDLE|SCRAPPED",
            message = "设备状态取值不合法，只能是 NORMAL / FAULT_REPAIR / IDLE / SCRAPPED")
    private String status;

    private String brand;

    private String model;

    private String serialNumber;

    private LocalDate purchaseDate;

    private LocalDate warrantyExpireDate;

    private String installationLocation;

    private String ipAddress;

    private String macAddress;

    private Long groupId;

    private Long responsibleUserId;

    private String supplier;

    private String department;

    private String remark;
}
