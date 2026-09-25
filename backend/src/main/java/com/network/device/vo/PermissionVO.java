package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限视图对象。
 * 字段名与前端 api/permission.ts 的 Permission 完全对齐：name/code/type/path/icon/sort/status/children。
 * 该 VO 同时用于「权限树」（role.vue 的分配权限弹窗，el-tree 取 name 作 label、id 作 node-key）
 * 与「权限列表」接口。
 */
@Data
public class PermissionVO implements Serializable {

    private Long id;

    private Long parentId;

    /** 权限名称（对应实体 permName） */
    private String name;

    /** 权限标识（对应实体 permKey） */
    private String code;

    /** 类型：1-菜单 2-按钮 */
    private Integer type;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Integer status;

    private List<PermissionVO> children;
}
