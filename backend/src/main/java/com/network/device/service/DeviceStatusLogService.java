package com.network.device.service;

import com.network.device.entity.DeviceStatusLog;
import com.network.device.vo.DeviceStatusLogVO;

import java.util.List;

public interface DeviceStatusLogService {

    List<DeviceStatusLog> listByDeviceId(Long deviceId);

    /** 按设备查询状态变更日志，返回字段名对齐前端的 VO（含操作人姓名） */
    List<DeviceStatusLogVO> listVOByDeviceId(Long deviceId);

    void saveLog(DeviceStatusLog log);
}
