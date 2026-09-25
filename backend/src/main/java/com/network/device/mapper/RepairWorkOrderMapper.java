package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.dto.StatisticsFilter;
import com.network.device.entity.RepairWorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RepairWorkOrderMapper extends BaseMapper<RepairWorkOrder> {

    @Select("SELECT * FROM repair_work_order WHERE work_order_no = #{workOrderNo} AND deleted = 0")
    RepairWorkOrder selectByWorkOrderNo(String workOrderNo);

    /**
     * 统计口径的过滤片段，拼接在各统计语句的 WHERE 之后。
     *
     * <p>本接口里的统计方法只被 {@code StatisticsServiceImpl} 使用，因此统一接收
     * {@link StatisticsFilter}：为 {@code null} 或某一项为 {@code null} 时，对应的
     * {@code <if>} 不成立，SQL 与「不加该条件」完全等价。
     *
     * <p>关于 {@code device_type}：{@code repair_work_order} 表上没有该列，
     * 只能通过 {@code device_id} 关联 {@code network_device} 判断。这里用 {@code EXISTS}
     * 而不是 JOIN，是为了不改变原有「一行工单就是一行」的分组语义
     * （JOIN 在设备被删/重复匹配时会放大计数）。
     */
    String STAT_FILTER =
            "<if test='f.startTime != null'> AND created_at &gt;= #{f.startTime}</if>"
                    + "<if test='f.endTime != null'> AND created_at &lt; #{f.endTime}</if>"
                    + "<if test='f.groupId != null'> AND group_id = #{f.groupId}</if>"
                    + "<if test='f.repairType != null'> AND repair_type = #{f.repairType}</if>"
                    + "<if test='f.repairUserId != null'> AND repair_user_id = #{f.repairUserId}</if>"
                    + "<if test='f.deviceType != null'> AND EXISTS (SELECT 1 FROM network_device d"
                    + " WHERE d.id = repair_work_order.device_id"
                    + " AND d.device_type = #{f.deviceType} AND d.deleted = 0)</if>";

    @Select("<script>SELECT COUNT(*) FROM repair_work_order WHERE deleted = 0"
            + STAT_FILTER + "</script>")
    Long countAll(@Param("f") StatisticsFilter filter);

    @Select("<script>SELECT COUNT(*) FROM repair_work_order WHERE status = #{status} AND deleted = 0"
            + STAT_FILTER + "</script>")
    Long countByStatus(@Param("status") String status, @Param("f") StatisticsFilter filter);

    @Select("<script>SELECT COUNT(*) FROM repair_work_order "
            + "WHERE status NOT IN ('ACCEPTED', 'CANCELLED', 'CLOSED') AND deleted = 0"
            + STAT_FILTER + "</script>")
    Long countPending(@Param("f") StatisticsFilter filter);

    /** 本月新增工单数（仪表盘「本月工单」用；此前误用 countAll 显示了历史总量） */
    @Select("<script>SELECT COUNT(*) FROM repair_work_order WHERE deleted = 0 "
            + "AND created_at &gt;= DATE_FORMAT(NOW(), '%Y-%m-01') "
            + "AND created_at &lt; DATE_FORMAT(NOW(), '%Y-%m-01') + INTERVAL 1 MONTH"
            + STAT_FILTER + "</script>")
    Long countThisMonth(@Param("f") StatisticsFilter filter);

    @Select("<script>SELECT COUNT(*) FROM repair_work_order WHERE overdue = 1 AND deleted = 0"
            + STAT_FILTER + "</script>")
    Long countOverdue(@Param("f") StatisticsFilter filter);

    @Select("<script>SELECT repair_type, COUNT(*) as count FROM repair_work_order "
            + "WHERE deleted = 0" + STAT_FILTER + " GROUP BY repair_type</script>")
    List<Map<String, Object>> countGroupByRepairType(@Param("f") StatisticsFilter filter);

    // 已删除 countMonthly()：与 countThisMonth() / countMonthlyTrend() 命名近似但语义不同
    // （前者是「当月总量」，后者是「近 12 个月按月分组」），且已无任何调用方，
    // 保留极易被误用，故移除。

    @Select("<script>SELECT * FROM repair_work_order WHERE deleted = 0" + STAT_FILTER
            + " ORDER BY created_at DESC LIMIT #{limit}</script>")
    List<RepairWorkOrder> selectRecent(@Param("limit") Integer limit,
                                       @Param("f") StatisticsFilter filter);

    @Select("<script>SELECT * FROM repair_work_order WHERE status = 'COMPLETED' AND deleted = 0"
            + STAT_FILTER + " ORDER BY completed_time DESC LIMIT #{limit}</script>")
    List<RepairWorkOrder> selectPendingAcceptance(@Param("limit") Integer limit,
                                                  @Param("f") StatisticsFilter filter);

    @Select("<script>SELECT AVG(TIMESTAMPDIFF(MINUTE, fault_time, completed_time)) "
            + "FROM repair_work_order "
            + "WHERE fault_time IS NOT NULL AND completed_time IS NOT NULL AND deleted = 0"
            + STAT_FILTER + "</script>")
    Double avgRepairDuration(@Param("f") StatisticsFilter filter);

    /**
     * 平均响应时长（分钟）：报修时间 -> 首次进入处理中。
     *
     * <p>子查询里不能给外层的 {@code repair_work_order} 起别名（如 {@code o}），
     * 否则 {@link #STAT_FILTER} 里那些不带前缀的列名（{@code created_at} / {@code group_id} …）
     * 会解析失败。因此统一写成「不加别名」的形式。
     */
    @Select("<script>SELECT AVG(TIMESTAMPDIFF(MINUTE, fault_time, "
            + "(SELECT MIN(l.operate_time) FROM repair_status_log l "
            + " WHERE l.work_order_id = repair_work_order.id"
            + " AND l.new_status = 'PROCESSING' AND l.deleted = 0))) "
            + "FROM repair_work_order WHERE deleted = 0 AND fault_time IS NOT NULL"
            + STAT_FILTER + "</script>")
    Double avgResponseDuration(@Param("f") StatisticsFilter filter);

    // 工单号格式为 WO-yyyyMMdd-%06d，序号在第 13 位起（共 6 位）。
    // 原先写 SUBSTRING(work_order_no, 16) 取到的是「后 3 位」，序号 <= 999 时恰好等价，
    // 到 1000 会退化成 0 从而生成重复单号。改用 SUBSTRING_INDEX 取最后一个 '-' 之后的部分，
    // 与位数/前缀长度都无关。
    //
    // 注意：这里**不能**加 deleted = 0。
    // uk_work_order_no 是普通唯一索引（不区分逻辑删除），被逻辑删除的工单在物理上仍然占用该单号。
    // 若只对 deleted = 0 的行取 MAX，删除掉当天最后一张工单后，下次生成会得到同一个单号，
    // 插入时直接撞唯一键报错。取号必须覆盖全部行，包括已逻辑删除的。
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(work_order_no, '-', -1) AS UNSIGNED)) FROM repair_work_order "
            + "WHERE work_order_no LIKE CONCAT('WO-', DATE_FORMAT(NOW(), '%Y%m%d'), '-%')")
    Integer getMaxWorkOrderNoOfDay();

    /** 按天统计近 30 天工单量 */
    @Select("<script>SELECT DATE_FORMAT(created_at, '%Y-%m-%d') as period, COUNT(*) as count "
            + "FROM repair_work_order WHERE deleted = 0 "
            + "AND created_at &gt;= DATE_SUB(CURDATE(), INTERVAL 30 DAY)"
            + STAT_FILTER + " GROUP BY period ORDER BY period</script>")
    List<Map<String, Object>> countDailyTrend(@Param("f") StatisticsFilter filter);

    /** 按周统计近 12 周工单量（以周一为起点） */
    @Select("<script>SELECT DATE_FORMAT(DATE_SUB(created_at, INTERVAL WEEKDAY(created_at) DAY), '%Y-%m-%d') as period, "
            + "COUNT(*) as count FROM repair_work_order "
            + "WHERE deleted = 0 AND created_at &gt;= DATE_SUB(CURDATE(), INTERVAL 12 WEEK)"
            + STAT_FILTER + " GROUP BY period ORDER BY period</script>")
    List<Map<String, Object>> countWeeklyTrend(@Param("f") StatisticsFilter filter);

    /**
     * 按月统计近 12 个月工单量。
     *
     * <p>必须带时间上界：原先没有 WHERE 时间条件，会返回建库以来的**全部**月份。
     * 日视图（30 天）与周视图（12 周）都是有界的，只有月视图无界，
     * 系统运行几年后该接口的返回量会持续膨胀，且与另两个视图的时间跨度不一致。
     */
    @Select("<script>SELECT DATE_FORMAT(created_at, '%Y-%m') as period, COUNT(*) as count "
            + "FROM repair_work_order WHERE deleted = 0 "
            + "AND created_at &gt;= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 11 MONTH)"
            + STAT_FILTER + " GROUP BY period ORDER BY period</script>")
    List<Map<String, Object>> countMonthlyTrend(@Param("f") StatisticsFilter filter);

    /** 按设备分组统计工单量（分组维修排名） */
    @Select("<script>SELECT group_id as groupId, COUNT(*) as count FROM repair_work_order "
            + "WHERE deleted = 0 AND group_id IS NOT NULL" + STAT_FILTER
            + " GROUP BY group_id ORDER BY count DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> countGroupByDeviceGroup(@Param("limit") Integer limit,
                                                      @Param("f") StatisticsFilter filter);

    /** 按设备统计工单量（设备维修排名） */
    @Select("<script>SELECT device_id as deviceId, device_code as deviceCode, device_name as deviceName, "
            + "COUNT(*) as count FROM repair_work_order "
            + "WHERE deleted = 0 AND device_id IS NOT NULL" + STAT_FILTER
            + " GROUP BY device_id, device_code, device_name ORDER BY count DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> countGroupByDevice(@Param("limit") Integer limit,
                                                 @Param("f") StatisticsFilter filter);
}
