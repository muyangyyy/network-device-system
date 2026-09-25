package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    @Select("SELECT perm_id FROM sys_role_permission WHERE role_id = #{roleId} AND deleted = 0")
    List<Long> selectPermIdsByRoleId(Long roleId);

    /** 批量查询多个角色的权限ID，用于列表回填，避免 N+1 */
    @Select("<script>SELECT role_id AS roleId, perm_id AS permId FROM sys_role_permission " +
            "WHERE deleted = 0 AND role_id IN " +
            "<foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectPermIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(Long roleId);
}
