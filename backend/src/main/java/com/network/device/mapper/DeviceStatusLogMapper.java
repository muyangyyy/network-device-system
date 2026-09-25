package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.DeviceStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceStatusLogMapper extends BaseMapper<DeviceStatusLog> {

    @Select("SELECT * FROM device_status_log WHERE device_id = #{deviceId} AND deleted = 0 ORDER BY operate_time DESC")
    List<DeviceStatusLog> selectByDeviceId(Long deviceId);

    @Select("SELECT * FROM device_status_log WHERE deleted = 0 ORDER BY operate_time DESC LIMIT #{limit}")
    List<DeviceStatusLog> selectRecent(Integer limit);
}
