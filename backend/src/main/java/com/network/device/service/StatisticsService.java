package com.network.device.service;

import com.network.device.dto.StatisticsQueryDTO;
import com.network.device.vo.DashboardVO;
import com.network.device.vo.DeviceRankingVO;
import com.network.device.vo.EfficiencyVO;
import com.network.device.vo.NameValueVO;
import com.network.device.vo.RepairTrendVO;

import java.util.List;

/**
 * 统计分析服务。
 *
 * <p>所有方法都接收 {@link StatisticsQueryDTO}，并把其中的筛选条件真正下推到 SQL。
 * 各条件生效范围如下（设备表上不存在的维度不会生效，不会静默拼出错误条件）：
 *
 * <table border="1">
 *   <caption>筛选条件生效范围</caption>
 *   <tr><th>条件</th><th>工单维度统计</th><th>设备维度统计</th></tr>
 *   <tr><td>startTime / endTime（按 created_at，左闭右开）</td><td>生效</td><td>生效</td></tr>
 *   <tr><td>groupId（精确匹配，不递归子分组）</td><td>生效</td><td>生效</td></tr>
 *   <tr><td>deviceType</td><td>生效（经 device_id 关联 network_device）</td><td>生效</td></tr>
 *   <tr><td>repairType</td><td>生效</td><td>不生效</td></tr>
 *   <tr><td>repairUserId</td><td>生效</td><td>不生效</td></tr>
 * </table>
 */
public interface StatisticsService {

    /** 首页驾驶舱概览：同时包含设备维度与工单维度两组指标 */
    DashboardVO getDashboardStats(StatisticsQueryDTO queryDTO);

    /** 维修类型分布：[{name, value}]（工单维度） */
    List<NameValueVO> getRepairTypeStats(StatisticsQueryDTO queryDTO);

    /** 维修趋势：{dates[], values[]}（工单维度，另受 period 影响） */
    RepairTrendVO getRepairTrendStats(StatisticsQueryDTO queryDTO);

    /** 分组维修排名：[{name, value}]（工单维度） */
    List<NameValueVO> getGroupRankingStats(StatisticsQueryDTO queryDTO);

    /** 设备维修排名：[{deviceCode, deviceName, repairCount}]（工单维度） */
    List<DeviceRankingVO> getDeviceRankingStats(StatisticsQueryDTO queryDTO);

    /** 维修效率统计（工单维度） */
    EfficiencyVO getEfficiencyStats(StatisticsQueryDTO queryDTO);

    /** 设备状态分布：[{name, value}]（设备维度） */
    List<NameValueVO> getDeviceStatusStats(StatisticsQueryDTO queryDTO);
}
