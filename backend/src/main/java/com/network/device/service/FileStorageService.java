package com.network.device.service;

import com.network.device.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * 保存上传文件到本地磁盘。
     *
     * @param file   上传的文件
     * @param subDir 业务子目录，如 repair / common
     * @return 保存结果（含可访问 URL）
     */
    UploadResultVO store(MultipartFile file, String subDir);

    /** 删除已保存的文件（按 store 返回的 URL） */
    void delete(String url);
}
