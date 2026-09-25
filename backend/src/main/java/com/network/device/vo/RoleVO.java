package com.network.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图对象。
 * 字段名与前端 api/role.ts 的 Role 完全对齐：name/code/description/permissionIds/createTime/updateTime。
 */
@Data
public class RoleVO implements Serializable {

    private Long id;

    /** 角色名称（对应实体 roleName） */
    private String name;

    /** 角色标识（对应实体 roleKey） */
    private String code;

    /** 描述（对应实体 remark） */
    private String description;

    /** 已分配的权限ID列表 */
    private List<Long> permissionIds;

    private Integer sort;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
