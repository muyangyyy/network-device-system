package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 维修趋势：x 轴日期序列 + y 轴数值序列 */
@Data
public class RepairTrendVO implements Serializable {

    private List<String> dates = new ArrayList<>();

    private List<Long> values = new ArrayList<>();
}
