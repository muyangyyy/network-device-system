package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.dto.StatisticsFilter;
import com.network.device.entity.NetworkDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface NetworkDeviceMapper extends BaseMapper<NetworkDevice> {

    @Select("SELECT * FROM network_device WHERE device_code = #{deviceCode} AND deleted = 0")
    NetworkDevice selectByDeviceCode(String deviceCode);

    @Select("SELECT * FROM network_device WHERE serial_number = #{serialNumber} AND deleted = 0")
    NetworkDevice selectBySerialNumber(String serialNumber);

    @Select("SELECT * FROM network_device WHERE ip_address = #{ipAddress} AND deleted = 0")
    NetworkDevice selectByIpAddress(String ipAddress);

    @Select("SELECT * FROM network_device WHERE mac_address = #{macAddress} AND deleted = 0")
    NetworkDevice selectByMacAddress(String macAddress);

    /**
     * 设备维度的统计过滤片段。
     *
     * <p>只包含 {@code network_device} 表上真实存在的维度：时间范围（{@code created_at}）、
     * 分组、设备类型。维修类型 / 维修人属于工单维度，在设备表上无法表达，
     * 因此设备维度的统计接口会忽略这两个筛选条件（详见 README「统计筛选口径」）。
     */
    String STAT_FILTER =
            "<if test='f.startTime != null'> AND created_at &gt;= #{f.startTime}</if>"
                    + "<if test='f.endTime != null'> AND created_at &lt; #{f.endTime}</if>"
                    + "<if test='f.groupId != null'> AND group_id = #{f.groupId}</if>"
                    + "<if test='f.deviceType != null'> AND device_type = #{f.deviceType}</if>";

    @Select("<script>SELECT COUNT(*) FROM network_device WHERE deleted = 0"
            + STAT_FILTER + "</script>")
    Long countAll(@Param("f") StatisticsFilter filter);

    @Select("<script>SELECT COUNT(*) FROM network_device WHERE status = #{status} AND deleted = 0"
            + STAT_FILTER + "</script>")
    Long countByStatus(@Param("status") String status, @Param("f") StatisticsFilter filter);

    @Select("<script>SELECT status, COUNT(*) as count FROM network_device "
            + "WHERE deleted = 0" + STAT_FILTER + " GROUP BY status</script>")
    List<Map<String, Object>> countGroupByStatus(@Param("f") StatisticsFilter filter);

    @Select("<script>SELECT device_type, COUNT(*) as count FROM network_device "
            + "WHERE deleted = 0" + STAT_FILTER + " GROUP BY device_type</script>")
    List<Map<String, Object>> countGroupByDeviceType(@Param("f") StatisticsFilter filter);

    // 设备编码格式为 NET-yyyyMMdd-%06d，序号在第 14 位起。
    // 原先写 SUBSTRING(device_code, 13) 会把分隔符一起取出来（"-000001"），
    // CAST 成 UNSIGNED 的结果不可预期。改用 SUBSTRING_INDEX 取最后一个 '-' 之后的部分。
    //
    // 注意：这里**不能**加 deleted = 0。
    // uk_device_code 是普通唯一索引（不区分逻辑删除），被逻辑删除的行在物理上仍然占用该编码。
    // 若只对 deleted = 0 的行取 MAX，删除掉当天最后一个编码后，下次生成会得到同一个编码，
    // 插入时直接撞唯一键报错。取号必须覆盖全部行，包括已逻辑删除的。
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(device_code, '-', -1) AS UNSIGNED)) FROM network_device "
            + "WHERE device_code LIKE CONCAT('NET-', DATE_FORMAT(NOW(), '%Y%m%d'), '-%')")
    Integer getMaxDeviceCodeOfDay();

    @Select("SELECT COUNT(*) FROM network_device WHERE group_id = #{groupId} AND deleted = 0")
    Long countByGroupId(Long groupId);

    @Select("SELECT COUNT(*) FROM network_device WHERE device_code = #{deviceCode} AND id != #{id} AND deleted = 0")
    Long countByDeviceCodeExcludeId(@Param("deviceCode") String deviceCode, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM network_device WHERE serial_number = #{serialNumber} AND id != #{id} AND deleted = 0")
    Long countBySerialNumberExcludeId(@Param("serialNumber") String serialNumber, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM network_device WHERE ip_address = #{ipAddress} AND id != #{id} AND deleted = 0")
    Long countByIpAddressExcludeId(@Param("ipAddress") String ipAddress, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM network_device WHERE mac_address = #{macAddress} AND id != #{id} AND deleted = 0")
    Long countByMacAddressExcludeId(@Param("macAddress") String macAddress, @Param("id") Long id);
}
