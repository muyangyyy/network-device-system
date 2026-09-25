package com.network.device.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 权限新增/修改请求体。
 * 字段名与前端 api/permission.ts 的 Permission 对齐（name/code/type/path/icon/sort/status），
 * 并用 @JsonAlias 兼容实体的历史命名（permName/permKey）。
 */
@Data
public class PermissionDTO implements Serializable {

    private Long id;

    private Long parentId;

    @NotBlank(message = "权限名称不能为空")
    @JsonAlias({"permName"})
    private String name;

    @JsonAlias({"permKey"})
    private String code;

    private Integer type;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Integer status;
}
