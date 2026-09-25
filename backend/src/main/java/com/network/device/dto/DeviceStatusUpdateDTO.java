package com.network.device.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备状态变更请求体。
 * 前端提交字段为 { status, reason }，此处用 @JsonAlias 兼容 reason -> remark。
 */
@Data
public class DeviceStatusUpdateDTO implements Serializable {

    @NotBlank(message = "设备状态不能为空")
    private String status;

    @JsonAlias({"reason"})
    private String remark;
}
