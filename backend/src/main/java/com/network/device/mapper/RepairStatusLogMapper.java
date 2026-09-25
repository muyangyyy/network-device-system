package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.RepairStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RepairStatusLogMapper extends BaseMapper<RepairStatusLog> {

    @Select("SELECT * FROM repair_status_log WHERE work_order_id = #{workOrderId} AND deleted = 0 ORDER BY operate_time")
    List<RepairStatusLog> selectByWorkOrderId(Long workOrderId);
}
