package com.network.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.annotation.OperationLog;
import com.network.device.common.PageResult;
import com.network.device.common.Result;
import com.network.device.entity.SysDict;
import com.network.device.entity.SysDictItem;
import com.network.device.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Tag(name = "字典管理", description = "字典及字典项管理接口")
@RestController
@RequestMapping("/api/dicts")
public class SysDictController {

    private final SysDictService sysDictService;

    public SysDictController(SysDictService sysDictService) {
        this.sysDictService = sysDictService;
    }

    @Operation(summary = "分页查询字典列表")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @GetMapping
    public Result<PageResult<SysDict>> listDicts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer size) {
        int effectivePageSize = pageSize != null ? pageSize : (size != null ? size : 10);
        Page<SysDict> result = sysDictService.listDicts(page, effectivePageSize);
        PageResult<SysDict> pageResult = new PageResult<>(result.getRecords(), result.getTotal(), page, effectivePageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取字典详情")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @GetMapping("/{id}")
    public Result<SysDict> getDictById(@PathVariable Long id) {
        return Result.success(sysDictService.getDictById(id));
    }

    @Operation(summary = "创建字典")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @PostMapping
    @OperationLog(module = "字典管理", operation = "创建字典")
    public Result<SysDict> createDict(@RequestBody SysDict dict) {
        return Result.success("创建成功", sysDictService.createDict(dict));
    }

    @Operation(summary = "更新字典")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @PutMapping("/{id}")
    @OperationLog(module = "字典管理", operation = "更新字典")
    public Result<SysDict> updateDict(@PathVariable Long id, @RequestBody SysDict dict) {
        return Result.success("更新成功", sysDictService.updateDict(id, dict));
    }

    @Operation(summary = "删除字典")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @DeleteMapping("/{id}")
    @OperationLog(module = "字典管理", operation = "删除字典")
    public Result<Void> deleteDict(@PathVariable Long id) {
        sysDictService.deleteDict(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "根据字典类型查询字典项列表")
    @PreAuthorize("@ss.hasAnyPermi('system:dict','device:list','device:view','device:add','device:edit','statistics:view')")
    @GetMapping("/{dictType}/items")
    public Result<List<SysDictItem>> listDictItems(@PathVariable String dictType) {
        return Result.success(sysDictService.listDictItemsByType(dictType));
    }

    @Operation(summary = "创建字典项")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @PostMapping("/{dictType}/items")
    @OperationLog(module = "字典管理", operation = "创建字典项")
    public Result<SysDictItem> createDictItem(@PathVariable String dictType, @RequestBody SysDictItem dictItem) {
        return Result.success("创建成功", sysDictService.createDictItem(dictType, dictItem));
    }

    @Operation(summary = "更新字典项")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @PutMapping("/items/{id}")
    @OperationLog(module = "字典管理", operation = "更新字典项")
    public Result<SysDictItem> updateDictItem(@PathVariable Long id, @RequestBody SysDictItem dictItem) {
        return Result.success("更新成功", sysDictService.updateDictItem(id, dictItem));
    }

    @Operation(summary = "删除字典项")
    @PreAuthorize("@ss.hasPermi('system:dict')")
    @DeleteMapping("/items/{id}")
    @OperationLog(module = "字典管理", operation = "删除字典项")
    public Result<Void> deleteDictItem(@PathVariable Long id) {
        sysDictService.deleteDictItem(id);
        return Result.success("删除成功");
    }
}
