package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.RepairOpticalDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RepairOpticalDetailMapper extends BaseMapper<RepairOpticalDetail> {

    @Select("SELECT * FROM repair_optical_detail WHERE work_order_id = #{workOrderId} AND deleted = 0")
    RepairOpticalDetail selectByWorkOrderId(Long workOrderId);
}
