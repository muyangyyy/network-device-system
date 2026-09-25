package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 树形结构通用 VO（设备分组 / 部门 共用）。
 * 字段名与前端约定保持一致：name / code / managerId / sort。
 */
@Data
public class DeviceGroupVO implements Serializable {

    private Long id;

    private Long parentId;

    /** 名称（设备分组名 / 部门名） */
    private String name;

    /** 编码（分组编码 / 部门编码） */
    private String code;

    private String groupType;

    /** 负责人用户ID */
    private Long managerId;

    /** 负责人姓名 */
    private String managerName;

    private String description;

    /** 排序 */
    private Integer sort;

    private Integer status;

    private List<DeviceGroupVO> children;
}
