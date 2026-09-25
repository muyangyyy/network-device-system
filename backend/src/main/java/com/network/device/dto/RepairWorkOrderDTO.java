package com.network.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RepairWorkOrderDTO implements Serializable {

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    private LocalDateTime faultTime;

    @NotBlank(message = "维修类型不能为空")
    private String repairType;

    @NotBlank(message = "故障描述不能为空")
    private String faultDescription;

    private String priority;

    private String remark;
}
