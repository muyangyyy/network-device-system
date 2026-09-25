package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录日志 VO —— 字段与前端 系统日志页 保持一致。
 */
@Data
public class LoginLogVO implements Serializable {

    private Long id;

    private String username;

    /** 登录IP */
    private String ip;

    /** 登录地点 */
    private String loginLocation;

    private String browser;

    private String os;

    /** 是否登录成功 */
    private Boolean success;

    /** 提示消息 */
    private String message;

    /** 登录时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
