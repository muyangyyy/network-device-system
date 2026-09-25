package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectRoleIdsByUserId(Long userId);

    /** 批量查询 user_id -> role_id，用于用户列表回填，避免 N+1 */
    @Select("<script>SELECT user_id AS userId, role_id AS roleId FROM sys_user_role " +
            "WHERE deleted = 0 AND user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectRoleIdsByUserIds(@Param("userIds") List<Long> userIds);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);
}
