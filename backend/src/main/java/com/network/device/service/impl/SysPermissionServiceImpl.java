package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.dto.PermissionDTO;
import com.network.device.entity.SysPermission;
import com.network.device.mapper.SysPermissionMapper;
import com.network.device.service.SysPermissionService;
import com.network.device.vo.PermissionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    public SysPermissionServiceImpl(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public List<PermissionVO> getPermissionTree() {
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getStatus, 1)
                        .orderByAsc(SysPermission::getSort));

        return buildTree(allPermissions, 0L);
    }

    private List<PermissionVO> buildTree(List<SysPermission> permissions, Long parentId) {
        List<PermissionVO> tree = new ArrayList<>();
        for (SysPermission perm : permissions) {
            if (!Objects.equals(perm.getParentId(), parentId)) {
                continue;
            }
            PermissionVO vo = toVO(perm);
            vo.setChildren(buildTree(permissions, perm.getId()));
            tree.add(vo);
        }
        return tree;
    }

    @Override
    public List<PermissionVO> listAllPermissions() {
        return sysPermissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermission>()
                                .orderByAsc(SysPermission::getSort))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionVO getPermissionById(Long id) {
        return toVO(requirePermission(id));
    }

    @Override
    @Transactional
    public PermissionVO createPermission(PermissionDTO permissionDTO) {
        SysPermission permission = new SysPermission();
        applyDTO(permission, permissionDTO);
        sysPermissionMapper.insert(permission);
        return toVO(permission);
    }

    @Override
    @Transactional
    public PermissionVO updatePermission(Long id, PermissionDTO permissionDTO) {
        SysPermission permission = requirePermission(id);
        applyDTO(permission, permissionDTO);
        // 环校验：把权限挂到自己或自己的后代下面，会让 getPermissionTree 的递归
        // 无限循环直到 StackOverflowError（与设备分组树同类问题）。
        // applyDTO 只在 dto.parentId 非空时才改 parentId，因此这里同样以它为准。
        if (permissionDTO != null && permissionDTO.getParentId() != null) {
            validateNoCycle(id, permission.getParentId());
        }
        permission.setId(id);
        sysPermissionMapper.updateById(permission);
        return toVO(permission);
    }

    /** 沿 parentId 祖先链向上走，若走到 id 自己说明会成环；visited 兜底历史数据已有的环 */
    private void validateNoCycle(Long id, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (id.equals(parentId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "父权限不能是自己");
        }
        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L && visited.add(cursor)) {
            if (cursor.equals(id)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "父权限不能是自己的子权限");
            }
            SysPermission parent = sysPermissionMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "父权限不存在");
            }
            cursor = parent.getParentId();
        }
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        requirePermission(id);
        // 子权限检查：直接删除会让子权限的 parentId 悬空，整棵子树从权限树里消失
        Long childCount = sysPermissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "该权限下有子权限，无法删除");
        }
        sysPermissionMapper.deleteById(id);
    }

    @Override
    public List<String> getPermKeysByUserId(Long userId) {
        List<SysPermission> permissions = sysPermissionMapper.selectByUserId(userId);
        return permissions.stream()
                .map(SysPermission::getPermKey)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ helpers

    private SysPermission requirePermission(Long id) {
        SysPermission permission = sysPermissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return permission;
    }

    /** 只覆盖前端提交的字段，null 表示不改动 */
    private void applyDTO(SysPermission permission, PermissionDTO dto) {
        if (dto.getName() != null) {
            permission.setPermName(dto.getName());
        }
        if (dto.getCode() != null) {
            permission.setPermKey(dto.getCode());
        }
        if (dto.getParentId() != null) {
            permission.setParentId(dto.getParentId());
        } else if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (dto.getType() != null) {
            permission.setType(dto.getType());
        }
        if (dto.getPath() != null) {
            permission.setPath(dto.getPath());
        }
        if (dto.getComponent() != null) {
            permission.setComponent(dto.getComponent());
        }
        if (dto.getIcon() != null) {
            permission.setIcon(dto.getIcon());
        }
        if (dto.getSort() != null) {
            permission.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            permission.setStatus(dto.getStatus());
        }
    }

    private PermissionVO toVO(SysPermission perm) {
        PermissionVO vo = new PermissionVO();
        vo.setId(perm.getId());
        vo.setParentId(perm.getParentId());
        vo.setName(perm.getPermName());
        vo.setCode(perm.getPermKey());
        vo.setType(perm.getType());
        vo.setPath(perm.getPath());
        vo.setComponent(perm.getComponent());
        vo.setIcon(perm.getIcon());
        vo.setSort(perm.getSort());
        vo.setStatus(perm.getStatus());
        return vo;
    }
}
