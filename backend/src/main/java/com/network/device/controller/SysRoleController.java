package com.network.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.annotation.OperationLog;
import com.network.device.common.PageResult;
import com.network.device.common.Result;
import com.network.device.dto.RoleDTO;
import com.network.device.service.SysRoleService;
import com.network.device.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Tag(name = "角色管理", description = "角色增删改查接口")
@RestController
@RequestMapping("/api/roles")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @Operation(summary = "分页查询角色列表")
    @PreAuthorize("@ss.hasAnyPermi('system:role','system:user')")
    @GetMapping
    public Result<PageResult<RoleVO>> listRoles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer size) {
        int effectivePageSize = pageSize != null ? pageSize : (size != null ? size : 10);
        Page<RoleVO> result = sysRoleService.listRoles(page, effectivePageSize);
        PageResult<RoleVO> pageResult =
                new PageResult<>(result.getRecords(), result.getTotal(), page, effectivePageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取所有角色")
    @PreAuthorize("@ss.hasAnyPermi('system:role','system:user')")
    @GetMapping("/all")
    public Result<List<RoleVO>> listAllRoles() {
        return Result.success(sysRoleService.listAllRoles());
    }

    @Operation(summary = "获取角色详情")
    @PreAuthorize("@ss.hasPermi('system:role')")
    @GetMapping("/{id}")
    public Result<RoleVO> getRoleById(@PathVariable Long id) {
        return Result.success(sysRoleService.getRoleById(id));
    }

    @Operation(summary = "创建角色")
    @PreAuthorize("@ss.hasPermi('system:role')")
    @PostMapping
    @OperationLog(module = "角色管理", operation = "创建角色")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        return Result.success("创建成功", sysRoleService.createRole(roleDTO));
    }

    @Operation(summary = "更新角色")
    @PreAuthorize("@ss.hasPermi('system:role')")
    @PutMapping("/{id}")
    @OperationLog(module = "角色管理", operation = "更新角色")
    public Result<RoleVO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        return Result.success("更新成功", sysRoleService.updateRole(id, roleDTO));
    }

    @Operation(summary = "删除角色")
    @PreAuthorize("@ss.hasPermi('system:role')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "角色管理", operation = "删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return Result.success("删除成功");
    }
}
