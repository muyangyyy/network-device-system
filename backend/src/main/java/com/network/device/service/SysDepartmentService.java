package com.network.device.service;

import com.network.device.entity.SysDepartment;
import com.network.device.vo.DeviceGroupVO;

import java.util.List;

public interface SysDepartmentService {

    List<DeviceGroupVO> getDepartmentTree();

    SysDepartment getDepartmentById(Long id);

    SysDepartment createDepartment(SysDepartment department);

    SysDepartment updateDepartment(Long id, SysDepartment department);

    void deleteDepartment(Long id);
}
