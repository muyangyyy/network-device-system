package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    Long countAll();

    @Select("SELECT COUNT(*) FROM sys_user WHERE status = #{status} AND deleted = 0")
    Long countByStatus(Integer status);

    /**
     * 统计同名用户数量，**包含已被逻辑删除的行**。
     *
     * <p>用途：新增用户前的唯一性预检。
     * {@code uk_username} 是普通唯一索引，不区分逻辑删除——被删除的用户在物理上仍占用该用户名。
     * 若预检只统计 {@code deleted = 0}（MyBatis-Plus 的 LambdaQueryWrapper 会自动追加该条件），
     * 删除一个用户后再创建同名用户时预检会通过，直到 INSERT 才撞唯一键报错，
     * 前端只能看到一个「数据已存在」的笼统提示，无法得知真正原因。
     * 因此预检必须覆盖全部行，才能给出准确的提示。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    Long countByUsernameIncludeDeleted(String username);
}
