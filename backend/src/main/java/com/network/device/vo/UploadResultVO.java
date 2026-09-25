package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

/** 文件上传结果 */
@Data
public class UploadResultVO implements Serializable {

    /** 可访问的相对地址，如 /uploads/repair/20260923/xxx.pdf */
    private String url;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件扩展名 */
    private String fileType;
}
