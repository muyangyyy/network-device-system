package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.RepairAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RepairAttachmentMapper extends BaseMapper<RepairAttachment> {

    @Select("SELECT * FROM repair_attachment WHERE work_order_id = #{workOrderId} AND deleted = 0")
    List<RepairAttachment> selectByWorkOrderId(Long workOrderId);
}
