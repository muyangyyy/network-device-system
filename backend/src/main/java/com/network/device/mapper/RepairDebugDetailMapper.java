package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.RepairDebugDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RepairDebugDetailMapper extends BaseMapper<RepairDebugDetail> {

    @Select("SELECT * FROM repair_debug_detail WHERE work_order_id = #{workOrderId} AND deleted = 0")
    RepairDebugDetail selectByWorkOrderId(Long workOrderId);
}
