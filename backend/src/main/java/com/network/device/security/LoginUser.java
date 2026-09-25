package com.network.device.security;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LoginUser implements Serializable {

    private Long id;
    private String username;
    private List<String> roles;

    public LoginUser() {
    }

    public LoginUser(Long id, String username, List<String> roles) {
        this.id = id;
        this.username = username;
        this.roles = roles;
    }
}
