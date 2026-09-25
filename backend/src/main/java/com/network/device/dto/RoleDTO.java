package com.network.device.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色新增/修改请求体。
 * 字段名与前端 api/role.ts 的 Role 保持一致（name/code/description/permissionIds）。
 * 同时用 @JsonAlias 兼容历史命名（roleName/roleKey/remark/permIds）。
 */
@Data
public class RoleDTO implements Serializable {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @JsonAlias({"roleName"})
    private String name;

    @NotBlank(message = "角色标识不能为空")
    @JsonAlias({"roleKey"})
    private String code;

    private Integer sort;

    private Integer status;

    @JsonAlias({"remark"})
    private String description;

    @JsonAlias({"permIds"})
    private List<Long> permissionIds;
}
