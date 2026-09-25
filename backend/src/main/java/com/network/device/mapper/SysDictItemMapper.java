package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {

    @Select("SELECT * FROM sys_dict_item WHERE dict_type = #{dictType} AND status = 1 AND deleted = 0 ORDER BY sort")
    List<SysDictItem> selectByDictType(String dictType);

    @Select("SELECT item_label FROM sys_dict_item WHERE dict_type = #{dictType} AND item_value = #{itemValue} AND deleted = 0")
    String selectLabelByTypeAndValue(@Param("dictType") String dictType, @Param("itemValue") String itemValue);
}
