package com.network.device.service.impl;

import com.network.device.dto.StatisticsFilter;
import com.network.device.dto.StatisticsQueryDTO;
import com.network.device.entity.DeviceGroup;
import com.network.device.entity.DeviceStatusLog;
import com.network.device.entity.RepairWorkOrder;
import com.network.device.mapper.DeviceGroupMapper;
import com.network.device.mapper.DeviceStatusLogMapper;
import com.network.device.mapper.NetworkDeviceMapper;
import com.network.device.mapper.RepairWorkOrderMapper;
import com.network.device.service.StatisticsService;
import com.network.device.vo.DashboardVO;
import com.network.device.vo.DeviceRankingVO;
import com.network.device.vo.EfficiencyVO;
import com.network.device.vo.NameValueVO;
import com.network.device.vo.RepairTrendVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final int RANKING_LIMIT = 10;

    private static final int RECENT_LIMIT = 10;

    private static final String DEFAULT_PERIOD = "daily";

    private static final Map<String, String> REPAIR_TYPE_LABELS = Map.of(
            "HARDWARE", "硬件维修",
            "DEBUG", "调试维修",
            "OPTICAL", "光路异常维修");

    private static final Map<String, String> DEVICE_STATUS_LABELS = Map.of(
            "NORMAL", "正常运行",
            "FAULT_REPAIR", "故障维修",
            "IDLE", "闲置",
            "SCRAPPED", "报废");

    private final NetworkDeviceMapper networkDeviceMapper;
    private final RepairWorkOrderMapper repairWorkOrderMapper;
    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final DeviceGroupMapper deviceGroupMapper;

    public StatisticsServiceImpl(NetworkDeviceMapper networkDeviceMapper,
                                 RepairWorkOrderMapper repairWorkOrderMapper,
                                 DeviceStatusLogMapper deviceStatusLogMapper,
                                 DeviceGroupMapper deviceGroupMapper) {
        this.networkDeviceMapper = networkDeviceMapper;
        this.repairWorkOrderMapper = repairWorkOrderMapper;
        this.deviceStatusLogMapper = deviceStatusLogMapper;
        this.deviceGroupMapper = deviceGroupMapper;
    }

    /**
     * 首页驾驶舱概览。
     *
     * <p>本方法同时返回「设备维度」与「工单维度」两组指标，因此传同一个 filter 给两类 Mapper：
     * 设备侧只认 时间范围 / 分组 / 设备类型，工单侧另外认 维修类型 / 维修人。
     * 过滤片段各自定义在对应的 Mapper 接口里，这里不需要做条件分支。
     */
    @Override
    public DashboardVO getDashboardStats(StatisticsQueryDTO queryDTO) {
        StatisticsFilter filter = StatisticsFilter.from(queryDTO);

        DashboardVO vo = new DashboardVO();

        vo.setTotalDevices(nz(networkDeviceMapper.countAll(filter)));
        vo.setNormalDevices(nz(networkDeviceMapper.countByStatus("NORMAL", filter)));
        vo.setFaultDevices(nz(networkDeviceMapper.countByStatus("FAULT_REPAIR", filter)));
        vo.setIdleDevices(nz(networkDeviceMapper.countByStatus("IDLE", filter)));
        vo.setScrappedDevices(nz(networkDeviceMapper.countByStatus("SCRAPPED", filter)));

        vo.setPendingOrders(nz(repairWorkOrderMapper.countPending(filter)));
        // 「本月工单数」——此前误用 countAll()，实际显示的是历史总量
        vo.setMonthlyOrders(nz(repairWorkOrderMapper.countThisMonth(filter)));
        vo.setOverdueOrders(nz(repairWorkOrderMapper.countOverdue(filter)));

        long totalOrders = nz(repairWorkOrderMapper.countAll(filter));
        long completedOrders = nz(repairWorkOrderMapper.countByStatus("COMPLETED", filter));
        long acceptedOrders = nz(repairWorkOrderMapper.countByStatus("ACCEPTED", filter));
        // CLOSED 是「已验收」之后的终态，必须计入已完成，否则工单一关闭完成率反而下降
        long closedOrders = nz(repairWorkOrderMapper.countByStatus("CLOSED", filter));

        vo.setAvgRepairTime(toHours(repairWorkOrderMapper.avgRepairDuration(filter)));
        vo.setCompletionRate(percent(completedOrders + acceptedOrders + closedOrders, totalOrders));
        vo.setAcceptanceRate(percent(acceptedOrders + closedOrders,
                completedOrders + acceptedOrders + closedOrders));

        Map<String, Long> deviceTypeDistribution = new HashMap<>();
        for (Map<String, Object> stat : networkDeviceMapper.countGroupByDeviceType(filter)) {
            deviceTypeDistribution.put(String.valueOf(stat.get("device_type")), toLong(stat.get("count")));
        }
        vo.setDeviceTypeDistribution(deviceTypeDistribution);

        Map<String, Long> repairTypeDistribution = new HashMap<>();
        for (Map<String, Object> stat : repairWorkOrderMapper.countGroupByRepairType(filter)) {
            repairTypeDistribution.put(String.valueOf(stat.get("repair_type")), toLong(stat.get("count")));
        }
        vo.setRepairTypeDistribution(repairTypeDistribution);

        vo.setRecentOrders(repairWorkOrderMapper.selectRecent(RECENT_LIMIT, filter).stream()
                .map(this::convertToRecentVO)
                .collect(Collectors.toList()));

        // 设备状态变更流是「最近发生了什么」的信息流，不按统计时间范围裁剪，
        // 否则选了历史区间后这块会整片空白。
        List<DeviceStatusLog> recentChanges = deviceStatusLogMapper.selectRecent(RECENT_LIMIT);
        vo.setRecentStatusChanges(recentChanges.stream()
                .map(this::convertToStatusLogVO)
                .collect(Collectors.toList()));

        vo.setPendingAcceptanceOrders(repairWorkOrderMapper
                .selectPendingAcceptance(RECENT_LIMIT, filter).stream()
                .map(this::convertToRecentVO)
                .collect(Collectors.toList()));

        return vo;
    }

    @Override
    public List<NameValueVO> getRepairTypeStats(StatisticsQueryDTO queryDTO) {
        return repairWorkOrderMapper.countGroupByRepairType(StatisticsFilter.from(queryDTO)).stream()
                .map(stat -> new NameValueVO(
                        label(REPAIR_TYPE_LABELS, (String) stat.get("repair_type")),
                        toLong(stat.get("count"))))
                .sorted(Comparator.comparingLong(NameValueVO::getValue).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public RepairTrendVO getRepairTrendStats(StatisticsQueryDTO queryDTO) {
        StatisticsFilter filter = StatisticsFilter.from(queryDTO);
        String rawPeriod = queryDTO == null ? null : queryDTO.getPeriod();
        String period = rawPeriod != null && !rawPeriod.isBlank()
                ? rawPeriod.trim().toLowerCase()
                : DEFAULT_PERIOD;

        List<Map<String, Object>> rows;
        if ("weekly".equals(period)) {
            rows = repairWorkOrderMapper.countWeeklyTrend(filter);
        } else if ("monthly".equals(period)) {
            rows = repairWorkOrderMapper.countMonthlyTrend(filter);
        } else {
            rows = repairWorkOrderMapper.countDailyTrend(filter);
        }

        RepairTrendVO vo = new RepairTrendVO();
        for (Map<String, Object> row : rows) {
            vo.getDates().add(String.valueOf(row.get("period")));
            vo.getValues().add(toLong(row.get("count")));
        }
        return vo;
    }

    @Override
    public List<NameValueVO> getGroupRankingStats(StatisticsQueryDTO queryDTO) {
        List<Map<String, Object>> rows =
                repairWorkOrderMapper.countGroupByDeviceGroup(RANKING_LIMIT, StatisticsFilter.from(queryDTO));

        Set<Long> groupIds = rows.stream()
                .map(row -> toNullableLong(row.get("groupId")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> groupNames = groupIds.isEmpty() ? Collections.emptyMap()
                : deviceGroupMapper.selectBatchIds(groupIds).stream()
                .collect(Collectors.toMap(DeviceGroup::getId, DeviceGroup::getGroupName, (a, b) -> a));

        return rows.stream()
                .map(row -> {
                    Long groupId = toNullableLong(row.get("groupId"));
                    String name = groupId == null
                            ? "未分组"
                            : groupNames.getOrDefault(groupId, "已删除分组#" + groupId);
                    return new NameValueVO(name, toLong(row.get("count")));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<DeviceRankingVO> getDeviceRankingStats(StatisticsQueryDTO queryDTO) {
        return repairWorkOrderMapper
                .countGroupByDevice(RANKING_LIMIT, StatisticsFilter.from(queryDTO)).stream()
                .map(row -> {
                    DeviceRankingVO vo = new DeviceRankingVO();
                    vo.setDeviceId(toNullableLong(row.get("deviceId")));
                    vo.setDeviceCode((String) row.get("deviceCode"));
                    vo.setDeviceName((String) row.get("deviceName"));
                    vo.setRepairCount(toLong(row.get("count")));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EfficiencyVO getEfficiencyStats(StatisticsQueryDTO queryDTO) {
        StatisticsFilter filter = StatisticsFilter.from(queryDTO);
        EfficiencyVO vo = new EfficiencyVO();

        long totalOrders = nz(repairWorkOrderMapper.countAll(filter));
        long completedOrders = nz(repairWorkOrderMapper.countByStatus("COMPLETED", filter));
        long acceptedOrders = nz(repairWorkOrderMapper.countByStatus("ACCEPTED", filter));
        // 同 getDashboardStats：CLOSED 属于已完成/已验收，不计入会让完成率随关单下降
        long closedOrders = nz(repairWorkOrderMapper.countByStatus("CLOSED", filter));
        long retryOrders = nz(repairWorkOrderMapper.countByStatus("REPAIR_AGAIN", filter));

        vo.setAvgRepairTime(toHours(repairWorkOrderMapper.avgRepairDuration(filter)));
        vo.setAvgResponseTime(toHours(repairWorkOrderMapper.avgResponseDuration(filter)));

        vo.setCompletionRate(percent(completedOrders + acceptedOrders + closedOrders, totalOrders));
        vo.setAcceptanceRate(percent(acceptedOrders + closedOrders,
                completedOrders + acceptedOrders + closedOrders));
        vo.setRetryRate(percent(retryOrders, totalOrders));
        vo.setFirstFixRate(percent(totalOrders - retryOrders, totalOrders));

        vo.setOverdueCount(nz(repairWorkOrderMapper.countOverdue(filter)));
        vo.setProcessingCount(nz(repairWorkOrderMapper.countByStatus("PROCESSING", filter)));

        return vo;
    }

    @Override
    public List<NameValueVO> getDeviceStatusStats(StatisticsQueryDTO queryDTO) {
        return networkDeviceMapper.countGroupByStatus(StatisticsFilter.from(queryDTO)).stream()
                .map(stat -> new NameValueVO(
                        label(DEVICE_STATUS_LABELS, (String) stat.get("status")),
                        toLong(stat.get("count"))))
                .sorted(Comparator.comparingLong(NameValueVO::getValue).reversed())
                .collect(Collectors.toList());
    }

    private String label(Map<String, String> labels, String code) {
        if (code == null) {
            return "未知";
        }
        return labels.getOrDefault(code, code);
    }

    /** 分钟 -> 小时，保留 1 位小数 */
    private static double toHours(Double minutes) {
        if (minutes == null) {
            return 0.0;
        }
        return Math.round(minutes / 60.0 * 10.0) / 10.0;
    }

    /** 百分比，保留 2 位小数 */
    private static double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((double) numerator / denominator * 10000.0) / 100.0;
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    private static Long toNullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private com.network.device.vo.RepairWorkOrderVO convertToRecentVO(RepairWorkOrder order) {
        com.network.device.vo.RepairWorkOrderVO vo = new com.network.device.vo.RepairWorkOrderVO();
        vo.setId(order.getId());
        vo.setWorkOrderNo(order.getWorkOrderNo());
        vo.setDeviceCode(order.getDeviceCode());
        vo.setDeviceName(order.getDeviceName());
        vo.setRepairType(order.getRepairType());
        vo.setStatus(order.getStatus());
        vo.setPriority(order.getPriority());
        vo.setCreateTime(order.getCreatedAt());
        vo.setFaultTime(order.getFaultTime());
        return vo;
    }

    private com.network.device.vo.DeviceStatusLogVO convertToStatusLogVO(DeviceStatusLog log) {
        com.network.device.vo.DeviceStatusLogVO vo = new com.network.device.vo.DeviceStatusLogVO();
        vo.setId(log.getId());
        vo.setDeviceId(log.getDeviceId());
        vo.setDeviceCode(log.getDeviceCode());
        vo.setFromStatus(log.getOriginalStatus());
        vo.setToStatus(log.getNewStatus());
        vo.setOperatorId(log.getOperatorId());
        vo.setReason(log.getRemark());
        vo.setCreateTime(log.getOperateTime() != null ? log.getOperateTime() : log.getCreatedAt());
        return vo;
    }
}
