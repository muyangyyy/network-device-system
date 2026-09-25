package com.network.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备分组 创建/更新 入参。
 * 字段名与前端保持一致：name / code / managerId / sort。
 */
@Data
public class DeviceGroupDTO implements Serializable {

    private Long id;

    private Long parentId;

    @NotBlank(message = "分组名称不能为空")
    private String name;

    private String code;

    private String groupType;

    private Long managerId;

    private String description;

    private Integer sort;

    private Integer status;
}
