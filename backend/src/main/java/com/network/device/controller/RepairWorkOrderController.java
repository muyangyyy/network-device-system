package com.network.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.annotation.OperationLog;
import com.network.device.common.PageResult;
import com.network.device.common.Result;
import com.network.device.dto.*;
import com.network.device.entity.RepairWorkOrder;
import com.network.device.service.DeviceStatusLogService;
import com.network.device.service.RepairWorkOrderService;
import com.network.device.vo.AttachmentVO;
import com.network.device.vo.OrderLogVO;
import com.network.device.vo.RepairWorkOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Tag(name = "维修工单管理", description = "维修工单全流程管理接口")
@RestController
@RequestMapping("/api/repairs")
public class RepairWorkOrderController {

    private final RepairWorkOrderService repairWorkOrderService;

    public RepairWorkOrderController(RepairWorkOrderService repairWorkOrderService) {
        this.repairWorkOrderService = repairWorkOrderService;
    }

    @Operation(summary = "分页查询工单列表")
    @PreAuthorize("@ss.hasAnyPermi('repair:list','repair:add','statistics:view')")
    @GetMapping
    public Result<PageResult<RepairWorkOrderVO>> listOrders(RepairOrderQueryDTO queryDTO) {
        Page<RepairWorkOrderVO> page = repairWorkOrderService.listOrders(queryDTO);
        PageResult<RepairWorkOrderVO> result = new PageResult<>(page.getRecords(), page.getTotal(), queryDTO.getPage(), queryDTO.getPageSize());
        return Result.success(result);
    }

    @Operation(summary = "获取工单详情")
    @PreAuthorize("@ss.hasAnyPermi('repair:view','repair:process','repair:accept','repair:add')")
    @GetMapping("/{id}")
    public Result<RepairWorkOrderVO> getOrderById(@PathVariable Long id) {
        return Result.success(repairWorkOrderService.getOrderVOById(id));
    }

    @Operation(summary = "创建工单")
    @PreAuthorize("@ss.hasPermi('repair:add')")
    @PostMapping
    @OperationLog(module = "工单管理", operation = "创建工单")
    public Result<RepairWorkOrderVO> createOrder(@Valid @RequestBody RepairWorkOrderDTO dto) {
        RepairWorkOrder created = repairWorkOrderService.createOrder(dto);
        return Result.success("创建成功", repairWorkOrderService.getOrderVOById(created.getId()));
    }

    @Operation(summary = "更新工单")
    @PreAuthorize("@ss.hasPermi('repair:add')")
    @PutMapping("/{id}")
    @OperationLog(module = "工单管理", operation = "更新工单")
    public Result<RepairWorkOrderVO> updateOrder(@PathVariable Long id, @RequestBody RepairWorkOrderDTO dto) {
        return Result.success("更新成功", repairWorkOrderService.updateOrder(id, dto));
    }

    @Operation(summary = "提交工单")
    @PreAuthorize("@ss.hasPermi('repair:add')")
    @PutMapping("/{id}/submit")
    @OperationLog(module = "工单管理", operation = "提交工单")
    public Result<Void> submitOrder(@PathVariable Long id) {
        repairWorkOrderService.submitOrder(id);
        return Result.success("提交成功");
    }

    @Operation(summary = "分配工单")
    @PreAuthorize("@ss.hasPermi('repair:assign')")
    @PutMapping("/{id}/assign")
    @OperationLog(module = "工单管理", operation = "分配工单")
    public Result<Void> assignOrder(@PathVariable Long id, @Valid @RequestBody AssignDTO dto) {
        repairWorkOrderService.assignOrder(id, dto);
        return Result.success("分配成功");
    }

    @Operation(summary = "开始处理")
    @PreAuthorize("@ss.hasPermi('repair:process')")
    @PutMapping("/{id}/start")
    @OperationLog(module = "工单管理", operation = "开始处理")
    public Result<Void> startProcessing(@PathVariable Long id) {
        repairWorkOrderService.startProcessing(id);
        return Result.success("已开始处理");
    }

    @Operation(summary = "完成工单")
    @PreAuthorize("@ss.hasPermi('repair:process')")
    @PutMapping("/{id}/complete")
    @OperationLog(module = "工单管理", operation = "完成工单")
    public Result<Void> completeOrder(@PathVariable Long id, @RequestBody CompleteDTO dto) {
        repairWorkOrderService.completeOrder(id, dto);
        return Result.success("已完成");
    }

    @Operation(summary = "验收工单")
    @PreAuthorize("@ss.hasPermi('repair:accept')")
    @PutMapping("/{id}/accept")
    @OperationLog(module = "工单管理", operation = "验收工单")
    public Result<Void> acceptOrder(@PathVariable Long id,
                                   @RequestBody(required = false) AcceptDTO dto) {
        repairWorkOrderService.acceptOrder(id, dto);
        return Result.success("验收通过");
    }

    @Operation(summary = "退回工单")
    @PreAuthorize("@ss.hasPermi('repair:accept')")
    @PutMapping("/{id}/reject")
    @OperationLog(module = "工单管理", operation = "退回工单")
    public Result<Void> rejectOrder(@PathVariable Long id, @Valid @RequestBody RejectDTO dto) {
        repairWorkOrderService.rejectOrder(id, dto);
        return Result.success("已退回");
    }

    @Operation(summary = "延期工单")
    @PreAuthorize("@ss.hasPermi('repair:process')")
    @PutMapping("/{id}/delay")
    @OperationLog(module = "工单管理", operation = "延期工单")
    public Result<Void> delayOrder(@PathVariable Long id, @Valid @RequestBody DelayDTO dto) {
        repairWorkOrderService.delayOrder(id, dto);
        return Result.success("已延期");
    }

    @Operation(summary = "返修工单")
    @PreAuthorize("@ss.hasPermi('repair:process')")
    @PutMapping("/{id}/repair-again")
    @OperationLog(module = "工单管理", operation = "返修工单")
    public Result<Void> repairAgain(@PathVariable Long id) {
        repairWorkOrderService.repairAgain(id);
        return Result.success("已发起返修");
    }

    @Operation(summary = "关闭工单")
    @PreAuthorize("@ss.hasPermi('repair:accept')")
    @PutMapping("/{id}/close")
    @OperationLog(module = "工单管理", operation = "关闭工单")
    public Result<Void> closeOrder(@PathVariable Long id) {
        repairWorkOrderService.closeOrder(id);
        return Result.success("已关闭");
    }

    @Operation(summary = "取消工单")
    @PreAuthorize("@ss.hasAnyPermi('repair:add','repair:assign')")
    @PutMapping("/{id}/cancel")
    @OperationLog(module = "工单管理", operation = "取消工单")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        repairWorkOrderService.cancelOrder(id);
        return Result.success("已取消");
    }

    @Operation(summary = "获取工单状态变更日志")
    @PreAuthorize("@ss.hasAnyPermi('repair:view','repair:process','repair:accept')")
    @GetMapping("/{id}/logs")
    public Result<List<OrderLogVO>> getOrderLogs(@PathVariable Long id) {
        return Result.success(repairWorkOrderService.getOrderLogs(id));
    }

    @Operation(summary = "获取工单附件列表")
    @PreAuthorize("@ss.hasAnyPermi('repair:view','repair:process','repair:accept')")
    @GetMapping("/{id}/attachments")
    public Result<List<AttachmentVO>> listAttachments(@PathVariable Long id) {
        return Result.success(repairWorkOrderService.listAttachments(id));
    }

    @Operation(summary = "上传工单附件")
    @PreAuthorize("@ss.hasPermi('repair:process')")
    @PostMapping("/{id}/attachments")
    @OperationLog(module = "工单管理", operation = "上传附件")
    public Result<AttachmentVO> uploadAttachment(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", repairWorkOrderService.uploadAttachment(id, file));
    }

    @Operation(summary = "删除工单", description = "级联删除附件、状态日志与维修明细，仅限管理员")
    @PreAuthorize("@ss.hasPermi('repair:delete')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "工单管理", operation = "删除工单")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        repairWorkOrderService.deleteOrder(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "删除工单附件")
    @PreAuthorize("@ss.hasPermi('repair:process')")
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @OperationLog(module = "工单管理", operation = "删除附件")
    public Result<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        repairWorkOrderService.deleteAttachment(id, attachmentId);
        return Result.success("删除成功");
    }

    @Operation(summary = "导出工单列表")
    @PreAuthorize("@ss.hasPermi('repair:export')")
    @GetMapping("/export")
    public Result<List<RepairWorkOrderVO>> exportOrders(RepairOrderQueryDTO queryDTO) {
        return Result.success(repairWorkOrderService.exportOrders(queryDTO));
    }
}
