package com.network.device.controller;

import com.network.device.annotation.OperationLog;
import com.network.device.common.Result;
import com.network.device.dto.PermissionDTO;
import com.network.device.service.SysPermissionService;
import com.network.device.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Tag(name = "权限管理", description = "权限增删改查接口")
@RestController
@RequestMapping("/api/permissions")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    public SysPermissionController(SysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    @Operation(summary = "获取权限树")
    @PreAuthorize("@ss.hasAnyPermi('system:permission','system:role')")
    @GetMapping("/tree")
    public Result<List<PermissionVO>> getPermissionTree() {
        return Result.success(sysPermissionService.getPermissionTree());
    }

    @Operation(summary = "获取所有权限列表")
    @PreAuthorize("@ss.hasPermi('system:permission')")
    @GetMapping
    public Result<List<PermissionVO>> listAllPermissions() {
        return Result.success(sysPermissionService.listAllPermissions());
    }

    @Operation(summary = "获取权限详情")
    @PreAuthorize("@ss.hasPermi('system:permission')")
    @GetMapping("/{id}")
    public Result<PermissionVO> getPermissionById(@PathVariable Long id) {
        return Result.success(sysPermissionService.getPermissionById(id));
    }

    @Operation(summary = "创建权限")
    @PreAuthorize("@ss.hasPermi('system:permission')")
    @PostMapping
    @OperationLog(module = "权限管理", operation = "创建权限")
    public Result<PermissionVO> createPermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        return Result.success("创建成功", sysPermissionService.createPermission(permissionDTO));
    }

    @Operation(summary = "更新权限")
    @PreAuthorize("@ss.hasPermi('system:permission')")
    @PutMapping("/{id}")
    @OperationLog(module = "权限管理", operation = "更新权限")
    // 这里**有意不加 @Valid**：SysPermissionServiceImpl.applyDTO 是「按非空字段覆盖」的部分更新
    // （if (dto.getName() != null) ...），而 PermissionDTO.name 上带 @NotBlank。
    // 一旦加 @Valid，只改 sort/icon 的调用也会被判定为「权限名称不能为空」而失败。
    // 正确做法是新增一个无必填约束的 PermissionUpdateDTO；在此之前保持不加校验。
    public Result<PermissionVO> updatePermission(@PathVariable Long id,
                                                 @RequestBody PermissionDTO permissionDTO) {
        return Result.success("更新成功", sysPermissionService.updatePermission(id, permissionDTO));
    }

    @Operation(summary = "删除权限")
    @PreAuthorize("@ss.hasPermi('system:permission')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "权限管理", operation = "删除权限")
    public Result<Void> deletePermission(@PathVariable Long id) {
        sysPermissionService.deletePermission(id);
        return Result.success("删除成功");
    }
}
