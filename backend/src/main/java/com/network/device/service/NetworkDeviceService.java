package com.network.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.dto.DeviceExcelDTO;
import com.network.device.dto.DeviceQueryDTO;
import com.network.device.dto.NetworkDeviceDTO;
import com.network.device.entity.NetworkDevice;
import com.network.device.vo.NetworkDeviceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NetworkDeviceService {

    Page<NetworkDeviceVO> listDevices(DeviceQueryDTO queryDTO);

    NetworkDevice getDeviceById(Long id);

    NetworkDeviceVO getDeviceVOById(Long id);

    NetworkDevice createDevice(NetworkDeviceDTO dto);

    NetworkDevice updateDevice(Long id, NetworkDeviceDTO dto);

    void deleteDevice(Long id);

    void updateStatus(Long id, String status, String remark);

    void batchUpdateGroup(List<Long> deviceIds, Long groupId);

    void importDevices(List<NetworkDeviceDTO> devices);

    /** 解析 Excel 并批量导入，返回成功导入的条数 */
    int importFromExcel(MultipartFile file);

    /** 按查询条件导出为 Excel 数据模型 */
    List<DeviceExcelDTO> exportExcelData(DeviceQueryDTO queryDTO);
}
