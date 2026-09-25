package com.network.device.service.impl;

import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.service.FileStorageService;
import com.network.device.vo.UploadResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "log", "zip", "rar", "7z");

    private static final DateTimeFormatter DATE_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Override
    public UploadResultVO store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed");
        String extension = StringUtils.getFilenameExtension(originalName);
        if (!StringUtils.hasText(extension) || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "不支持的文件类型：" + (StringUtils.hasText(extension) ? extension : "未知"));
        }
        extension = extension.toLowerCase(Locale.ROOT);

        String safeSubDir = StringUtils.hasText(subDir) ? subDir : "common";
        String dateDir = LocalDate.now().format(DATE_DIR_FORMAT);

        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(safeSubDir).resolve(dateDir).normalize();
        if (!targetDir.startsWith(baseDir)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "非法的上传目录");
        }

        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", targetDir, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "创建上传目录失败");
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetFile = targetDir.resolve(storedName).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "非法的文件名");
        }

        try {
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("保存上传文件失败: {}", targetFile, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "保存文件失败");
        }

        UploadResultVO vo = new UploadResultVO();
        vo.setUrl("/uploads/" + safeSubDir + "/" + dateDir + "/" + storedName);
        vo.setFileName(originalName);
        vo.setFileSize(file.getSize());
        vo.setFileType(extension);
        return vo;
    }

    @Override
    public void delete(String url) {
        if (!StringUtils.hasText(url) || !url.startsWith("/uploads/")) {
            return;
        }
        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path target = baseDir.resolve(url.substring("/uploads/".length())).normalize();
        if (!target.startsWith(baseDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", target, e);
        }
    }
}
