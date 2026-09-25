package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LoginVO implements Serializable {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    /** 前端统一使用 nickname，取值同 realName */
    private String nickname;

    private List<String> roles;

    private List<String> permissions;
}
