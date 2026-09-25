package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修效率统计。
 * 时间类指标单位为「小时」，比率类指标单位为「百分比数值」。
 */
@Data
public class EfficiencyVO implements Serializable {

    /** 平均响应时间（小时）：报修 -> 开始处理 */
    private Double avgResponseTime = 0.0;

    /** 平均维修时长（小时）：报修 -> 完成 */
    private Double avgRepairTime = 0.0;

    /** 完成率（%）：(已完成 + 已验收) / 总工单 */
    private Double completionRate = 0.0;

    /** 验收通过率（%）：已验收 / (已完成 + 已验收) */
    private Double acceptanceRate = 0.0;

    /** 超时工单数 */
    private Long overdueCount = 0L;

    /** 处理中工单数 */
    private Long processingCount = 0L;

    /** 一次修复率（%）：未发生返修的工单占比 */
    private Double firstFixRate = 0.0;

    /** 返修率（%）：返修工单占比 */
    private Double retryRate = 0.0;
}
