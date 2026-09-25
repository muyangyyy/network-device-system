package com.network.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.common.PageResult;
import com.network.device.common.Result;
import com.network.device.dto.LogQueryDTO;
import com.network.device.service.LoginLogService;
import com.network.device.service.OperationLogService;
import com.network.device.vo.LoginLogVO;
import com.network.device.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "系统日志", description = "操作日志与登录日志查询接口")
@RestController
@RequestMapping("/api/system-logs")
public class SystemLogController {

    private final OperationLogService operationLogService;
    private final LoginLogService loginLogService;

    public SystemLogController(OperationLogService operationLogService, LoginLogService loginLogService) {
        this.operationLogService = operationLogService;
        this.loginLogService = loginLogService;
    }

    @Operation(summary = "分页查询操作日志")
    @PreAuthorize("@ss.hasPermi('system:log')")
    @GetMapping
    public Result<PageResult<OperationLogVO>> listOperationLogs(LogQueryDTO queryDTO) {
        Page<OperationLogVO> page = operationLogService.listLogVO(queryDTO);
        PageResult<OperationLogVO> result = new PageResult<>(
                page.getRecords(), page.getTotal(), queryDTO.getPage(), queryDTO.getPageSize());
        return Result.success(result);
    }

    @Operation(summary = "分页查询登录日志")
    @PreAuthorize("@ss.hasPermi('system:loginLog')")
    @GetMapping("/login")
    public Result<PageResult<LoginLogVO>> listLoginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer size) {
        int effectivePageSize = pageSize != null ? pageSize : (size != null ? size : 10);
        Page<LoginLogVO> result = loginLogService.listLoginLogs(username, page, effectivePageSize);
        PageResult<LoginLogVO> pageResult = new PageResult<>(
                result.getRecords(), result.getTotal(), page, effectivePageSize);
        return Result.success(pageResult);
    }
}
