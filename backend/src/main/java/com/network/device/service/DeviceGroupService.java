package com.network.device.service;

import com.network.device.dto.DeviceGroupDTO;
import com.network.device.entity.DeviceGroup;
import com.network.device.vo.DeviceGroupVO;

import java.util.List;

public interface DeviceGroupService {

    List<DeviceGroupVO> getGroupTree();

    DeviceGroup getGroupById(Long id);

    DeviceGroup createGroup(DeviceGroupDTO dto);

    DeviceGroup updateGroup(Long id, DeviceGroupDTO dto);

    void deleteGroup(Long id);

    void updateManager(Long id, Long managerUserId);

    void migrateDevices(List<Long> deviceIds, Long targetGroupId);
}
