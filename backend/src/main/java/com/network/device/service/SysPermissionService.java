package com.network.device.service;

import com.network.device.dto.PermissionDTO;
import com.network.device.vo.PermissionVO;

import java.util.List;

public interface SysPermissionService {

    List<PermissionVO> getPermissionTree();

    List<PermissionVO> listAllPermissions();

    PermissionVO getPermissionById(Long id);

    PermissionVO createPermission(PermissionDTO permissionDTO);

    PermissionVO updatePermission(Long id, PermissionDTO permissionDTO);

    void deletePermission(Long id);

    List<String> getPermKeysByUserId(Long userId);
}
