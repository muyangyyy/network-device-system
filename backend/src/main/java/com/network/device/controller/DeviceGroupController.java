package com.network.device.controller;

import com.network.device.annotation.OperationLog;
import com.network.device.common.Result;
import com.network.device.dto.DeviceGroupDTO;
import com.network.device.entity.DeviceGroup;
import com.network.device.service.DeviceGroupService;
import com.network.device.vo.DeviceGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Tag(name = "设备分组管理", description = "设备分组增删改查接口")
@RestController
@RequestMapping("/api/device-groups")
public class DeviceGroupController {

    private final DeviceGroupService deviceGroupService;

    public DeviceGroupController(DeviceGroupService deviceGroupService) {
        this.deviceGroupService = deviceGroupService;
    }

    @Operation(summary = "获取设备分组树")
    @PreAuthorize("@ss.hasAnyPermi('group:list','device:list','device:add','device:edit','statistics:view')")
    @GetMapping("/tree")
    public Result<List<DeviceGroupVO>> getGroupTree() {
        return Result.success(deviceGroupService.getGroupTree());
    }

    @Operation(summary = "获取所有分组")
    @PreAuthorize("@ss.hasPermi('group:list')")
    @GetMapping
    public Result<List<DeviceGroupVO>> listAllGroups() {
        return Result.success(deviceGroupService.getGroupTree());
    }

    @Operation(summary = "获取分组详情")
    @PreAuthorize("@ss.hasPermi('group:list')")
    @GetMapping("/{id}")
    public Result<DeviceGroup> getGroupById(@PathVariable Long id) {
        return Result.success(deviceGroupService.getGroupById(id));
    }

    @Operation(summary = "创建设备分组")
    @PreAuthorize("@ss.hasPermi('group:list')")
    @PostMapping
    @OperationLog(module = "设备分组管理", operation = "创建分组")
    public Result<DeviceGroup> createGroup(@Valid @RequestBody DeviceGroupDTO dto) {
        return Result.success("创建成功", deviceGroupService.createGroup(dto));
    }

    @Operation(summary = "更新设备分组")
    @PreAuthorize("@ss.hasPermi('group:list')")
    @PutMapping("/{id}")
    @OperationLog(module = "设备分组管理", operation = "更新分组")
    public Result<DeviceGroup> updateGroup(@PathVariable Long id, @Valid @RequestBody DeviceGroupDTO dto) {
        return Result.success("更新成功", deviceGroupService.updateGroup(id, dto));
    }

    @Operation(summary = "删除设备分组")
    @PreAuthorize("@ss.hasPermi('group:list')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "设备分组管理", operation = "删除分组")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        deviceGroupService.deleteGroup(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "更新分组管理员")
    @PreAuthorize("@ss.hasPermi('group:list')")
    @PutMapping("/{id}/manager")
    @OperationLog(module = "设备分组管理", operation = "更新管理员")
    public Result<Void> updateManager(@PathVariable Long id, @RequestParam Long managerId) {
        deviceGroupService.updateManager(id, managerId);
        return Result.success("更新成功");
    }

    @Operation(summary = "批量迁移设备到目标分组")
    @PreAuthorize("@ss.hasPermi('group:list')")
    @PostMapping("/migrate-devices")
    @OperationLog(module = "设备分组管理", operation = "迁移设备")
    public Result<Void> migrateDevices(@RequestBody MigrateRequest request) {
        deviceGroupService.migrateDevices(request.getDeviceIds(), request.getTargetGroupId());
        return Result.success("迁移成功");
    }

    @lombok.Data
    public static class MigrateRequest {
        private List<Long> deviceIds;
        private Long targetGroupId;
    }
}
