package com.network.device.controller;

import com.network.device.annotation.OperationLog;
import com.network.device.common.Result;
import com.network.device.entity.SysDepartment;
import com.network.device.service.SysDepartmentService;
import com.network.device.vo.DeviceGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Tag(name = "部门管理", description = "部门增删改查接口")
@RestController
@RequestMapping("/api/departments")
public class SysDepartmentController {

    private final SysDepartmentService sysDepartmentService;

    public SysDepartmentController(SysDepartmentService sysDepartmentService) {
        this.sysDepartmentService = sysDepartmentService;
    }

    @Operation(summary = "获取部门树")
    @PreAuthorize("@ss.hasAnyPermi('system:department','system:user')")
    @GetMapping("/tree")
    public Result<List<DeviceGroupVO>> getDepartmentTree() {
        return Result.success(sysDepartmentService.getDepartmentTree());
    }

    @Operation(summary = "获取所有部门")
    @PreAuthorize("@ss.hasPermi('system:department')")
    @GetMapping
    public Result<List<DeviceGroupVO>> listAllDepartments() {
        return Result.success(sysDepartmentService.getDepartmentTree());
    }

    @Operation(summary = "获取部门详情")
    @PreAuthorize("@ss.hasPermi('system:department')")
    @GetMapping("/{id}")
    public Result<SysDepartment> getDepartmentById(@PathVariable Long id) {
        return Result.success(sysDepartmentService.getDepartmentById(id));
    }

    @Operation(summary = "创建部门")
    @PreAuthorize("@ss.hasPermi('system:department')")
    @PostMapping
    @OperationLog(module = "部门管理", operation = "创建部门")
    public Result<SysDepartment> createDepartment(@RequestBody SysDepartment department) {
        return Result.success("创建成功", sysDepartmentService.createDepartment(department));
    }

    @Operation(summary = "更新部门")
    @PreAuthorize("@ss.hasPermi('system:department')")
    @PutMapping("/{id}")
    @OperationLog(module = "部门管理", operation = "更新部门")
    public Result<SysDepartment> updateDepartment(@PathVariable Long id, @RequestBody SysDepartment department) {
        return Result.success("更新成功", sysDepartmentService.updateDepartment(id, department));
    }

    @Operation(summary = "删除部门")
    @PreAuthorize("@ss.hasPermi('system:department')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "部门管理", operation = "删除部门")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        sysDepartmentService.deleteDepartment(id);
        return Result.success("删除成功");
    }
}
