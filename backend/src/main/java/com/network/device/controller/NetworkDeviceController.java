package com.network.device.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.annotation.OperationLog;
import com.network.device.common.PageResult;
import com.network.device.common.Result;
import com.network.device.dto.DeviceExcelDTO;
import com.network.device.dto.DeviceQueryDTO;
import com.network.device.dto.DeviceStatusUpdateDTO;
import com.network.device.dto.NetworkDeviceDTO;
import com.network.device.dto.RepairOrderQueryDTO;
import com.network.device.entity.NetworkDevice;
import com.network.device.service.DeviceStatusLogService;
import com.network.device.service.NetworkDeviceService;
import com.network.device.service.RepairWorkOrderService;
import com.network.device.vo.DeviceStatusLogVO;
import com.network.device.vo.NetworkDeviceVO;
import com.network.device.vo.RepairWorkOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "网络设备管理", description = "网络设备增删改查及批量操作接口")
@RestController
@RequestMapping("/api/devices")
public class NetworkDeviceController {

    private final NetworkDeviceService networkDeviceService;
    private final RepairWorkOrderService repairWorkOrderService;
    private final DeviceStatusLogService deviceStatusLogService;

    public NetworkDeviceController(NetworkDeviceService networkDeviceService,
                                   RepairWorkOrderService repairWorkOrderService,
                                   DeviceStatusLogService deviceStatusLogService) {
        this.networkDeviceService = networkDeviceService;
        this.repairWorkOrderService = repairWorkOrderService;
        this.deviceStatusLogService = deviceStatusLogService;
    }

    @Operation(summary = "分页查询设备列表")
    @PreAuthorize("@ss.hasAnyPermi('device:list','repair:add','group:list','statistics:view')")
    @GetMapping
    public Result<PageResult<NetworkDeviceVO>> listDevices(DeviceQueryDTO queryDTO) {
        Page<NetworkDeviceVO> page = networkDeviceService.listDevices(queryDTO);
        PageResult<NetworkDeviceVO> result = new PageResult<>(
                page.getRecords(), page.getTotal(), queryDTO.getPage(), queryDTO.getPageSize());
        return Result.success(result);
    }

    @Operation(summary = "获取设备详情")
    @PreAuthorize("@ss.hasAnyPermi('device:view','device:edit','repair:add')")
    @GetMapping("/{id}")
    public Result<NetworkDeviceVO> getDeviceById(@PathVariable Long id) {
        return Result.success(networkDeviceService.getDeviceVOById(id));
    }

    @Operation(summary = "创建设备")
    @PreAuthorize("@ss.hasPermi('device:add')")
    @PostMapping
    @OperationLog(module = "设备管理", operation = "创建设备")
    public Result<NetworkDevice> createDevice(@Valid @RequestBody NetworkDeviceDTO dto) {
        return Result.success("创建成功", networkDeviceService.createDevice(dto));
    }

    @Operation(summary = "更新设备")
    @PreAuthorize("@ss.hasPermi('device:edit')")
    @PutMapping("/{id}")
    @OperationLog(module = "设备管理", operation = "更新设备")
    public Result<NetworkDevice> updateDevice(@PathVariable Long id, @Valid @RequestBody NetworkDeviceDTO dto) {
        return Result.success("更新成功", networkDeviceService.updateDevice(id, dto));
    }

    @Operation(summary = "删除设备")
    @PreAuthorize("@ss.hasPermi('device:delete')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "设备管理", operation = "删除设备")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        networkDeviceService.deleteDevice(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "更新设备状态")
    @PreAuthorize("@ss.hasPermi('device:edit')")
    @PutMapping("/{id}/status")
    @OperationLog(module = "设备管理", operation = "更新设备状态")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody DeviceStatusUpdateDTO dto) {
        networkDeviceService.updateStatus(id, dto.getStatus(), dto.getRemark());
        return Result.success("状态更新成功");
    }

    @Operation(summary = "批量更新设备分组")
    @PreAuthorize("@ss.hasPermi('device:edit')")
    @PutMapping("/batch-group")
    @OperationLog(module = "设备管理", operation = "批量更新分组")
    public Result<Void> batchUpdateGroup(@RequestBody BatchGroupRequest request) {
        networkDeviceService.batchUpdateGroup(request.getDeviceIds(), request.getGroupId());
        return Result.success("分组更新成功");
    }

    @Operation(summary = "导入设备（Excel 文件）")
    @PreAuthorize("@ss.hasPermi('device:import')")
    @PostMapping("/import")
    @OperationLog(module = "设备管理", operation = "导入设备")
    public Result<Integer> importDevices(@RequestParam("file") MultipartFile file) {
        int count = networkDeviceService.importFromExcel(file);
        return Result.success("成功导入 " + count + " 条设备数据", count);
    }

    @Operation(summary = "导出设备列表（Excel 文件）")
    @PreAuthorize("@ss.hasPermi('device:export')")
    @GetMapping("/export")
    public void exportDevices(DeviceQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        List<DeviceExcelDTO> rows = networkDeviceService.exportExcelData(queryDTO);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode("设备列表_" + LocalDate.now(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), DeviceExcelDTO.class)
                .sheet("设备列表")
                .doWrite(rows);
    }

    @Operation(summary = "获取设备关联的维修工单")
    @PreAuthorize("@ss.hasAnyPermi('device:view','repair:list')")
    @GetMapping("/{id}/repairs")
    public Result<PageResult<RepairWorkOrderVO>> getDeviceRepairs(@PathVariable Long id) {
        RepairOrderQueryDTO repairQuery = new RepairOrderQueryDTO();
        repairQuery.setPage(1);
        repairQuery.setPageSize(1000);
        Page<RepairWorkOrderVO> page = repairWorkOrderService.listOrdersByDeviceId(id, repairQuery);
        PageResult<RepairWorkOrderVO> result = new PageResult<>(
                page.getRecords(), page.getTotal(), repairQuery.getPage(), repairQuery.getPageSize());
        return Result.success(result);
    }

    @Operation(summary = "获取设备状态变更日志")
    @PreAuthorize("@ss.hasAnyPermi('device:view','device:edit')")
    @GetMapping("/{id}/status-logs")
    public Result<PageResult<DeviceStatusLogVO>> getStatusLogs(@PathVariable Long id,
                                                               @RequestParam(defaultValue = "1") Integer page,
                                                               @RequestParam(required = false) Integer pageSize,
                                                               @RequestParam(required = false) Integer size) {
        int effectivePageSize = pageSize != null ? pageSize : (size != null ? size : 10);
        List<DeviceStatusLogVO> logs = deviceStatusLogService.listVOByDeviceId(id);
        int from = Math.min((page - 1) * effectivePageSize, logs.size());
        int to = Math.min(from + effectivePageSize, logs.size());
        PageResult<DeviceStatusLogVO> result = new PageResult<>(
                logs.subList(from, to), (long) logs.size(), page, effectivePageSize);
        return Result.success(result);
    }

    @lombok.Data
    public static class BatchGroupRequest {
        private List<Long> deviceIds;
        private Long groupId;
    }
}
