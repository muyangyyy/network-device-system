package com.network.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.annotation.OperationLog;
import com.network.device.common.PageResult;
import com.network.device.common.Result;
import com.network.device.dto.UserDTO;
import com.network.device.dto.UserQueryDTO;
import com.network.device.entity.SysUser;
import com.network.device.service.SysUserService;
import com.network.device.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "用户管理", description = "用户增删改查接口")
@RestController
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Operation(summary = "分页查询用户列表")
    @PreAuthorize("@ss.hasAnyPermi('system:user','device:list','device:view','device:add','device:edit','repair:list','repair:view','repair:add','group:list','statistics:view')")
    @GetMapping
    public Result<PageResult<UserVO>> listUsers(UserQueryDTO queryDTO) {
        Page<UserVO> page = sysUserService.listUserVOs(queryDTO);
        PageResult<UserVO> result =
                new PageResult<>(page.getRecords(), page.getTotal(), queryDTO.getPage(), queryDTO.getPageSize());
        return Result.success(result);
    }

    @Operation(summary = "获取用户详情")
    @PreAuthorize("@ss.hasPermi('system:user')")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.success(sysUserService.getUserVOById(id));
    }

    @Operation(summary = "创建用户")
    @PreAuthorize("@ss.hasPermi('system:user')")
    @PostMapping
    @OperationLog(module = "用户管理", operation = "创建用户")
    public Result<UserVO> createUser(@Valid @RequestBody UserDTO userDTO) {
        SysUser created = sysUserService.createUser(userDTO);
        return Result.success("创建成功", sysUserService.getUserVOById(created.getId()));
    }

    @Operation(summary = "更新用户")
    @PreAuthorize("@ss.hasPermi('system:user')")
    @PutMapping("/{id}")
    @OperationLog(module = "用户管理", operation = "更新用户")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        sysUserService.updateUser(id, userDTO);
        return Result.success("更新成功", sysUserService.getUserVOById(id));
    }

    @Operation(summary = "删除用户")
    @PreAuthorize("@ss.hasPermi('system:user')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "用户管理", operation = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "更新用户状态")
    @PreAuthorize("@ss.hasPermi('system:user')")
    @PutMapping("/{id}/status")
    @OperationLog(module = "用户管理", operation = "更新用户状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateDTO dto) {
        sysUserService.updateStatus(id, dto.getStatus());
        return Result.success("状态更新成功");
    }

    @Operation(summary = "重置用户密码（返回随机明文，仅展示一次）")
    @PreAuthorize("@ss.hasPermi('system:user')")
    @PutMapping("/{id}/reset-password")
    @OperationLog(module = "用户管理", operation = "重置密码")
    public Result<String> resetPassword(@PathVariable Long id) {
        String rawPassword = sysUserService.resetPassword(id);
        return Result.success("密码已重置为随机值，请妥善转交用户", rawPassword);
    }

    @lombok.Data
    public static class StatusUpdateDTO {
        private Integer status;
    }
}
