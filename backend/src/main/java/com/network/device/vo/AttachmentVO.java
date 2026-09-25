package com.network.device.vo;

import lombok.Data;

import java.io.Serializable;

/** 工单附件 */
@Data
public class AttachmentVO implements Serializable {

    private Long id;

    private Long workOrderId;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    /** 上传时间 yyyy-MM-dd HH:mm:ss */
    private String uploadTime;

    private Long uploaderId;

    private String uploaderName;
}
