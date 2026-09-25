package com.network.device.service.impl;

import com.network.device.entity.DeviceStatusLog;
import com.network.device.entity.SysUser;
import com.network.device.mapper.DeviceStatusLogMapper;
import com.network.device.mapper.SysUserMapper;
import com.network.device.service.DeviceStatusLogService;
import com.network.device.vo.DeviceStatusLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeviceStatusLogServiceImpl implements DeviceStatusLogService {

    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final SysUserMapper sysUserMapper;

    public DeviceStatusLogServiceImpl(DeviceStatusLogMapper deviceStatusLogMapper,
                                      SysUserMapper sysUserMapper) {
        this.deviceStatusLogMapper = deviceStatusLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<DeviceStatusLog> listByDeviceId(Long deviceId) {
        return deviceStatusLogMapper.selectByDeviceId(deviceId);
    }

    @Override
    public List<DeviceStatusLogVO> listVOByDeviceId(Long deviceId) {
        List<DeviceStatusLog> logs = listByDeviceId(deviceId);
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量解析操作人姓名，避免 N+1
        Set<Long> operatorIds = logs.stream()
                .map(DeviceStatusLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> operatorNames = operatorIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(),
                        (a, b) -> a));

        return logs.stream()
                .map(log -> toVO(log, operatorNames))
                .collect(Collectors.toList());
    }

    @Override
    public void saveLog(DeviceStatusLog log) {
        deviceStatusLogMapper.insert(log);
    }

    private DeviceStatusLogVO toVO(DeviceStatusLog log, Map<Long, String> operatorNames) {
        DeviceStatusLogVO vo = new DeviceStatusLogVO();
        vo.setId(log.getId());
        vo.setDeviceId(log.getDeviceId());
        vo.setDeviceCode(log.getDeviceCode());
        vo.setFromStatus(log.getOriginalStatus());
        vo.setToStatus(log.getNewStatus());
        vo.setOperatorId(log.getOperatorId());
        // created_by 存的是用户名，优先用它；否则按 operatorId 反查
        vo.setOperatorName(StringUtils.hasText(log.getCreatedBy())
                ? log.getCreatedBy()
                : (log.getOperatorId() == null ? null : operatorNames.get(log.getOperatorId())));
        vo.setReason(log.getRemark());
        vo.setCreateTime(log.getOperateTime() != null ? log.getOperateTime() : log.getCreatedAt());
        return vo;
    }
}
