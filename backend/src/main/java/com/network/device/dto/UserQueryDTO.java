package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户查询参数。
 * 前端使用 nickname / size，后端统一使用 realName / pageSize，此处做兼容。
 */
@Data
public class UserQueryDTO implements Serializable {

    private String username;

    private String realName;

    private String phone;

    private Long departmentId;

    private Integer status;

    private Integer page = 1;

    private Integer pageSize = 10;

    /** 兼容前端 nickname 参数 */
    public void setNickname(String nickname) {
        this.realName = nickname;
    }

    /** 兼容前端 size 参数 */
    public void setSize(Integer size) {
        if (size != null) {
            this.pageSize = size;
        }
    }
}
