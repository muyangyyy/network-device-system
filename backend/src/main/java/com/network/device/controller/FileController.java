package com.network.device.controller;

import com.network.device.common.Result;
import com.network.device.service.FileStorageService;
import com.network.device.vo.UploadResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "文件上传", description = "通用文件上传接口")
@RestController
@RequestMapping("/api")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "上传文件")
    @PreAuthorize("@ss.hasAnyPermi('device:import','repair:add','repair:process')")
    @PostMapping("/upload")
    public Result<UploadResultVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", fileStorageService.store(file, "common"));
    }
}
