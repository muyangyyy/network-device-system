package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.dto.RoleDTO;
import com.network.device.entity.SysRole;
import com.network.device.entity.SysRolePermission;
import com.network.device.mapper.SysRoleMapper;
import com.network.device.mapper.SysRolePermissionMapper;
import com.network.device.service.SysRoleService;
import com.network.device.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public SysRoleServiceImpl(SysRoleMapper sysRoleMapper, SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @Override
    public Page<RoleVO> listRoles(Integer page, Integer pageSize) {
        Page<SysRole> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRole::getSort);
        Page<SysRole> result = sysRoleMapper.selectPage(pageParam, wrapper);

        // 批量回填 permissionIds，避免 N+1
        List<SysRole> records = result.getRecords();
        Map<Long, List<Long>> permMap = loadPermIds(
                records.stream().map(SysRole::getId).collect(Collectors.toList()));

        List<RoleVO> vos = records.stream()
                .map(role -> toVO(role, permMap.getOrDefault(role.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        Page<RoleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public List<RoleVO> listAllRoles() {
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
        return roles.stream()
                .map(role -> toVO(role, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    @Override
    public RoleVO getRoleById(Long id) {
        SysRole role = requireRole(id);
        return toVO(role, sysRolePermissionMapper.selectPermIdsByRoleId(id));
    }

    @Override
    @Transactional
    public RoleVO createRole(RoleDTO roleDTO) {
        // 唯一性预检必须覆盖「已逻辑删除」的行：uk_role_key 是普通唯一索引，不区分逻辑删除，
        // 被删除的角色在物理上仍占用该 role_key。
        // 用 selectCount(LambdaQueryWrapper) 会被自动追加 deleted = 0，从而漏判。
        Long exists = sysRoleMapper.countByRoleKeyIncludeDeleted(roleDTO.getCode());
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DATA_EXIST.getCode(),
                    "角色标识已存在（可能属于已删除的角色，请更换标识）");
        }

        SysRole role = new SysRole();
        role.setRoleName(roleDTO.getName());
        role.setRoleKey(roleDTO.getCode());
        role.setSort(roleDTO.getSort());
        role.setStatus(roleDTO.getStatus() != null ? roleDTO.getStatus() : 1);
        role.setRemark(roleDTO.getDescription());
        sysRoleMapper.insert(role);

        List<Long> permIds = roleDTO.getPermissionIds();
        if (permIds != null && !permIds.isEmpty()) {
            insertRolePermissions(role.getId(), permIds);
        }

        return toVO(role, permIds == null ? Collections.emptyList() : permIds);
    }

    @Override
    @Transactional
    public RoleVO updateRole(Long id, RoleDTO roleDTO) {
        SysRole role = requireRole(id);

        if (StringUtils.hasText(roleDTO.getName())) {
            role.setRoleName(roleDTO.getName());
        }
        if (StringUtils.hasText(roleDTO.getCode())) {
            role.setRoleKey(roleDTO.getCode());
        }
        if (roleDTO.getSort() != null) {
            role.setSort(roleDTO.getSort());
        }
        if (roleDTO.getStatus() != null) {
            role.setStatus(roleDTO.getStatus());
        }
        if (roleDTO.getDescription() != null) {
            role.setRemark(roleDTO.getDescription());
        }
        sysRoleMapper.updateById(role);

        if (roleDTO.getPermissionIds() != null) {
            sysRolePermissionMapper.deleteByRoleId(id);
            insertRolePermissions(id, roleDTO.getPermissionIds());
        }

        return toVO(role, sysRolePermissionMapper.selectPermIdsByRoleId(id));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        requireRole(id);
        sysRoleMapper.deleteById(id);
        sysRolePermissionMapper.deleteByRoleId(id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permIds) {
        requireRole(roleId);
        sysRolePermissionMapper.deleteByRoleId(roleId);
        if (permIds != null && !permIds.isEmpty()) {
            insertRolePermissions(roleId, permIds);
        }
    }

    // ------------------------------------------------------------------ helpers

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return role;
    }

    private void insertRolePermissions(Long roleId, List<Long> permIds) {
        for (Long permId : permIds) {
            if (permId == null) {
                continue;
            }
            SysRolePermission rolePermission = new SysRolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermId(permId);
            sysRolePermissionMapper.insert(rolePermission);
        }
    }

    /** 批量查询 roleId -> permIds 映射 */
    private Map<Long, List<Long>> loadPermIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = sysRolePermissionMapper.selectPermIdsByRoleIds(roleIds);
        Map<Long, List<Long>> map = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            Long roleId = toLong(row.get("roleId"));
            Long permId = toLong(row.get("permId"));
            if (roleId == null || permId == null) {
                continue;
            }
            map.computeIfAbsent(roleId, k -> new ArrayList<>()).add(permId);
        }
        return map;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    private RoleVO toVO(SysRole role, List<Long> permIds) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setName(role.getRoleName());
        vo.setCode(role.getRoleKey());
        vo.setDescription(role.getRemark());
        vo.setPermissionIds(permIds == null ? Collections.emptyList() : permIds);
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreatedAt());
        vo.setUpdateTime(role.getUpdatedAt());
        return vo;
    }
}
