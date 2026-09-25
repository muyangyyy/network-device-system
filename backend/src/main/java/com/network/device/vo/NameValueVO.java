package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

/** 通用「名称-数值」统计项，用于饼图 / 条形图 */
@Data
public class NameValueVO implements Serializable {

    private String name;

    private Long value;

    public NameValueVO() {
    }

    public NameValueVO(String name, Long value) {
        this.name = name;
        this.value = value;
    }
}
