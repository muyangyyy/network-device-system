package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.DeviceGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceGroupMapper extends BaseMapper<DeviceGroup> {

    @Select("SELECT * FROM device_group WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<DeviceGroup> selectByParentId(Long parentId);

    @Select("SELECT * FROM device_group WHERE group_code = #{groupCode} AND deleted = 0")
    DeviceGroup selectByGroupCode(String groupCode);

    @Select("SELECT COUNT(*) FROM device_group WHERE parent_id = #{parentId} AND deleted = 0")
    Long countByParentId(Long parentId);

    @Select("SELECT COUNT(*) FROM network_device WHERE group_id = #{groupId} AND deleted = 0")
    Long countDevicesByGroupId(Long groupId);

    @Select("SELECT COUNT(*) FROM repair_work_order WHERE group_id = #{groupId} AND deleted = 0")
    Long countOrdersByGroupId(Long groupId);
}
