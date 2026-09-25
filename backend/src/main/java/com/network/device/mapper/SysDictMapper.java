package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    @Select("SELECT * FROM sys_dict WHERE dict_type = #{dictType} AND deleted = 0")
    SysDict selectByDictType(String dictType);

    /**
     * 统计同 dict_type 的字典数量，**包含已被逻辑删除的行**。
     *
     * <p>用途：新增字典前的唯一性预检。理由同
     * {@link SysUserMapper#countByUsernameIncludeDeleted(String)}：
     * {@code uk_dict_type} 不区分逻辑删除，删除字典后其 dict_type 仍被物理占用。
     * 注意 {@link #selectByDictType(String)} 带 {@code deleted = 0}，
     * 用于唯一性判断会漏掉已删除行，故这里单独提供一个不带该条件的统计方法。
     */
    @Select("SELECT COUNT(*) FROM sys_dict WHERE dict_type = #{dictType}")
    Long countByDictTypeIncludeDeleted(String dictType);
}
