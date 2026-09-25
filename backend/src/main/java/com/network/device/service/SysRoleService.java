package com.network.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.dto.RoleDTO;
import com.network.device.vo.RoleVO;

import java.util.List;

public interface SysRoleService {

    Page<RoleVO> listRoles(Integer page, Integer pageSize);

    List<RoleVO> listAllRoles();

    RoleVO getRoleById(Long id);

    RoleVO createRole(RoleDTO roleDTO);

    RoleVO updateRole(Long id, RoleDTO roleDTO);

    void deleteRole(Long id);

    void assignPermissions(Long roleId, List<Long> permIds);
}
