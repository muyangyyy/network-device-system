package com.network.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.dto.LogQueryDTO;
import com.network.device.entity.OperationLog;
import com.network.device.vo.OperationLogVO;

public interface OperationLogService {

    void saveLog(OperationLog log);

    Page<OperationLog> listLogs(String module, Integer page, Integer pageSize);

    /** 按前端日志页需求查询操作日志（含操作人/操作类型/时间范围过滤） */
    Page<OperationLogVO> listLogVO(LogQueryDTO query);
}
