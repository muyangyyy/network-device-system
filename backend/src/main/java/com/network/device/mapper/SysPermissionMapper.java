package com.network.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.network.device.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("SELECT * FROM sys_permission WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort")
    List<SysPermission> selectByParentId(Long parentId);

    /**
     * 查询用户通过其角色获得的权限。
     *
     * <p>必须同时过滤 {@code r.status = 1}：{@code sys_role} 有启用/禁用开关，
     * 但这里原先只过滤了权限自身的 {@code p.status}，没有过滤角色状态，
     * 导致**把角色禁用后，该角色授予的权限依然生效**——「禁用角色」这个功能形同虚设。
     * 该结果用于构建登录用户的权限键集合，是接口鉴权的唯一数据来源，因此必须收紧。
     */
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "INNER JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1 AND r.status = 1 " +
            "AND p.deleted = 0 AND rp.deleted = 0 AND ur.deleted = 0 AND r.deleted = 0 " +
            "ORDER BY p.sort")
    List<SysPermission> selectByUserId(Long userId);

    @Select("SELECT perm_id FROM sys_role_permission WHERE role_id = #{roleId} AND deleted = 0")
    List<Long> selectPermIdsByRoleId(Long roleId);
}
