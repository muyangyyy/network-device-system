package com.network.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String realName;

    /** 前端统一使用 nickname，取值同 realName */
    private String nickname;

    private String phone;

    private String email;

    private String avatar;

    private Long departmentId;

    private String departmentName;

    /** 角色ID列表（用户管理页表单用） */
    private List<Long> roleIds;

    /** 角色名称列表（用户管理页列表展示用） */
    private List<String> roleNames;

    /** 角色标识列表（登录/当前用户接口用） */
    private List<String> roles;

    /** 权限标识列表（登录/当前用户接口用） */
    private List<String> permissions;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
