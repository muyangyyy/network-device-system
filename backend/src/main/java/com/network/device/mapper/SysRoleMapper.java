package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户的角色标识（ADMIN / OPERATOR / READONLY）。
     *
     * <p>必须过滤 {@code r.status = 1}：角色有启用/禁用开关，被禁用的角色不应再向用户
     * 授予任何身份。原先只过滤了 {@code deleted}，导致禁用角色后用户依旧持有该角色标识
     * （进而通过 {@code selectByUserId} 拿到该角色的权限），「禁用角色」不生效。
     */
    @Select("SELECT r.role_key FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0 AND ur.deleted = 0")
    List<String> selectRoleKeysByUserId(Long userId);

    @Select("SELECT * FROM sys_role WHERE role_key = #{roleKey} AND deleted = 0")
    SysRole selectByRoleKey(String roleKey);

    @Select("SELECT * FROM sys_role WHERE id IN " +
            "(SELECT role_id FROM sys_role_permission WHERE perm_id = #{permId} AND deleted = 0) " +
            "AND deleted = 0")
    List<SysRole> selectRolesByPermId(Long permId);

    /**
     * 统计同 role_key 的角色数量，**包含已被逻辑删除的行**。
     *
     * <p>用途：新增角色前的唯一性预检。理由同
     * {@link SysUserMapper#countByUsernameIncludeDeleted(String)}：
     * {@code uk_role_key} 不区分逻辑删除，删除角色后其 role_key 仍被物理占用。
     */
    @Select("SELECT COUNT(*) FROM sys_role WHERE role_key = #{roleKey}")
    Long countByRoleKeyIncludeDeleted(String roleKey);
}
