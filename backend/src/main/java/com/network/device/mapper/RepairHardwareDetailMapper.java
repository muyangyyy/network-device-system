package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.RepairHardwareDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RepairHardwareDetailMapper extends BaseMapper<RepairHardwareDetail> {

    @Select("SELECT * FROM repair_hardware_detail WHERE work_order_id = #{workOrderId} AND deleted = 0")
    RepairHardwareDetail selectByWorkOrderId(Long workOrderId);
}
