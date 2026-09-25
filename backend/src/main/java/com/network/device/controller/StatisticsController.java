package com.network.device.controller;

import com.network.device.common.Result;
import com.network.device.dto.StatisticsQueryDTO;
import com.network.device.service.StatisticsService;
import com.network.device.vo.DashboardVO;
import com.network.device.vo.DeviceRankingVO;
import com.network.device.vo.EfficiencyVO;
import com.network.device.vo.NameValueVO;
import com.network.device.vo.RepairTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * 统计分析接口。
 *
 * <p>全部接口共用一组查询参数（{@link StatisticsQueryDTO}）：{@code startTime} / {@code endTime}
 * / {@code groupId} / {@code deviceType} / {@code repairType} / {@code repairUserId}。
 *
 * <p>参数生效范围：时间范围按 {@code created_at} 左闭右开；{@code deviceType} 在工单维度
 * 通过 {@code device_id} 关联 {@code network_device} 判断；{@code repairType} 与
 * {@code repairUserId} 只对工单维度统计生效（设备维度接口会忽略这两项）。
 */
@Tag(name = "统计分析", description = "数据看板和统计报表接口")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(summary = "获取数据看板概览",
            description = "设备维度指标吃 时间范围/分组/设备类型；工单维度指标额外吃 维修类型/维修人")
    @PreAuthorize("@ss.hasPermi('statistics:view')")
    @GetMapping("/overview")
    public Result<DashboardVO> getOverview(StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getDashboardStats(queryDTO));
    }

    @Operation(summary = "获取维修类型分布", description = "工单维度，支持全部筛选条件")
    @PreAuthorize("@ss.hasPermi('statistics:view')")
    @GetMapping("/repair-type")
    public Result<List<NameValueVO>> getRepairTypeStats(StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getRepairTypeStats(queryDTO));
    }

    @Operation(summary = "获取维修趋势",
            description = "工单维度，支持全部筛选条件；period=daily|weekly|monthly 控制粒度")
    @PreAuthorize("@ss.hasPermi('statistics:view')")
    @GetMapping("/repair-trend")
    public Result<RepairTrendVO> getRepairTrendStats(StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getRepairTrendStats(queryDTO));
    }

    @Operation(summary = "获取分组维修排名", description = "工单维度，支持全部筛选条件")
    @PreAuthorize("@ss.hasPermi('statistics:view')")
    @GetMapping("/group-ranking")
    public Result<List<NameValueVO>> getGroupRankingStats(StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getGroupRankingStats(queryDTO));
    }

    @Operation(summary = "获取设备维修排名", description = "工单维度，支持全部筛选条件")
    @PreAuthorize("@ss.hasPermi('statistics:view')")
    @GetMapping("/device-ranking")
    public Result<List<DeviceRankingVO>> getDeviceRankingStats(StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getDeviceRankingStats(queryDTO));
    }

    @Operation(summary = "获取效率统计", description = "工单维度，支持全部筛选条件")
    @PreAuthorize("@ss.hasPermi('statistics:view')")
    @GetMapping("/efficiency")
    public Result<EfficiencyVO> getEfficiencyStats(StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getEfficiencyStats(queryDTO));
    }

    @Operation(summary = "获取设备状态统计",
            description = "设备维度，只支持 时间范围/分组/设备类型")
    @PreAuthorize("@ss.hasPermi('statistics:view')")
    @GetMapping("/device-status")
    public Result<List<NameValueVO>> getDeviceStatusStats(StatisticsQueryDTO queryDTO) {
        return Result.success(statisticsService.getDeviceStatusStats(queryDTO));
    }
}
