package com.network.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserDTO implements Serializable {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private Long departmentId;

    private List<Long> roleIds;

    private Integer status;

    /** 兼容前端 nickname 字段，映射到 realName */
    public void setNickname(String nickname) {
        this.realName = nickname;
    }
}
