package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    @Select("SELECT * FROM sys_department WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort")
    List<SysDepartment> selectByParentId(Long parentId);

    @Select("SELECT * FROM sys_department WHERE id = #{id} AND deleted = 0")
    SysDepartment selectByIdOnly(Long id);
}
