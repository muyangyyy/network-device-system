package com.network.device.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.dto.DeviceExcelDTO;
import com.network.device.dto.DeviceQueryDTO;
import com.network.device.dto.NetworkDeviceDTO;
import com.network.device.entity.DeviceGroup;
import com.network.device.entity.DeviceStatusLog;
import com.network.device.entity.NetworkDevice;
import com.network.device.entity.RepairWorkOrder;
import com.network.device.entity.SysUser;
import com.network.device.mapper.DeviceGroupMapper;
import com.network.device.mapper.DeviceStatusLogMapper;
import com.network.device.mapper.NetworkDeviceMapper;
import com.network.device.mapper.RepairWorkOrderMapper;
import com.network.device.mapper.SysUserMapper;
import com.network.device.service.NetworkDeviceService;
import com.network.device.vo.NetworkDeviceVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NetworkDeviceServiceImpl implements NetworkDeviceService {

    /** 导出上限，避免全表导出导致内存溢出 */
    private static final int EXPORT_MAX_ROWS = 100000;

    private static final Map<String, String> STATUS_NAMES = Map.of(
            "NORMAL", "正常运行",
            "FAULT_REPAIR", "故障维修",
            "IDLE", "闲置",
            "SCRAPPED", "报废");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-M-d");

    private final NetworkDeviceMapper networkDeviceMapper;
    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final RepairWorkOrderMapper repairWorkOrderMapper;
    private final DeviceGroupMapper deviceGroupMapper;
    private final SysUserMapper sysUserMapper;

    public NetworkDeviceServiceImpl(NetworkDeviceMapper networkDeviceMapper,
                                    DeviceStatusLogMapper deviceStatusLogMapper,
                                    RepairWorkOrderMapper repairWorkOrderMapper,
                                    DeviceGroupMapper deviceGroupMapper,
                                    SysUserMapper sysUserMapper) {
        this.networkDeviceMapper = networkDeviceMapper;
        this.deviceStatusLogMapper = deviceStatusLogMapper;
        this.repairWorkOrderMapper = repairWorkOrderMapper;
        this.deviceGroupMapper = deviceGroupMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public Page<NetworkDeviceVO> listDevices(DeviceQueryDTO queryDTO) {
        int pageNo = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10;

        Page<NetworkDevice> page = new Page<>(pageNo, pageSize);
        Page<NetworkDevice> result = networkDeviceMapper.selectPage(page, buildWrapper(queryDTO));

        Page<NetworkDeviceVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(enrich(result.getRecords()));
        return voPage;
    }

    /** 组装查询条件 */
    private LambdaQueryWrapper<NetworkDevice> buildWrapper(DeviceQueryDTO queryDTO) {
        LambdaQueryWrapper<NetworkDevice> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getDeviceCode())) {
            wrapper.like(NetworkDevice::getDeviceCode, queryDTO.getDeviceCode());
        }
        if (StringUtils.hasText(queryDTO.getDeviceName())) {
            wrapper.like(NetworkDevice::getDeviceName, queryDTO.getDeviceName());
        }
        if (StringUtils.hasText(queryDTO.getDeviceType())) {
            wrapper.eq(NetworkDevice::getDeviceType, queryDTO.getDeviceType());
        }
        if (StringUtils.hasText(queryDTO.getBrand())) {
            wrapper.eq(NetworkDevice::getBrand, queryDTO.getBrand());
        }
        if (StringUtils.hasText(queryDTO.getSerialNumber())) {
            wrapper.like(NetworkDevice::getSerialNumber, queryDTO.getSerialNumber());
        }
        if (StringUtils.hasText(queryDTO.getIpAddress())) {
            wrapper.like(NetworkDevice::getIpAddress, queryDTO.getIpAddress());
        }
        if (StringUtils.hasText(queryDTO.getMacAddress())) {
            wrapper.like(NetworkDevice::getMacAddress, queryDTO.getMacAddress());
        }
        if (queryDTO.getGroupId() != null) {
            wrapper.eq(NetworkDevice::getGroupId, queryDTO.getGroupId());
        }
        if (queryDTO.getResponsibleUserId() != null) {
            wrapper.eq(NetworkDevice::getResponsibleUserId, queryDTO.getResponsibleUserId());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(NetworkDevice::getStatus, queryDTO.getStatus());
        }
        if (StringUtils.hasText(queryDTO.getInstallationLocation())) {
            wrapper.like(NetworkDevice::getInstallationLocation, queryDTO.getInstallationLocation());
        }
        if (StringUtils.hasText(queryDTO.getPurchaseDateStart())) {
            wrapper.ge(NetworkDevice::getPurchaseDate, LocalDate.parse(queryDTO.getPurchaseDateStart()));
        }
        if (StringUtils.hasText(queryDTO.getPurchaseDateEnd())) {
            wrapper.le(NetworkDevice::getPurchaseDate, LocalDate.parse(queryDTO.getPurchaseDateEnd()));
        }
        if (StringUtils.hasText(queryDTO.getWarrantyExpireDateStart())) {
            wrapper.ge(NetworkDevice::getWarrantyExpireDate, LocalDate.parse(queryDTO.getWarrantyExpireDateStart()));
        }
        if (StringUtils.hasText(queryDTO.getWarrantyExpireDateEnd())) {
            wrapper.le(NetworkDevice::getWarrantyExpireDate, LocalDate.parse(queryDTO.getWarrantyExpireDateEnd()));
        }

        wrapper.orderByDesc(NetworkDevice::getCreatedAt);
        return wrapper;
    }

    /** 批量补齐分组名与负责人姓名，避免 N+1 查询 */
    private List<NetworkDeviceVO> enrich(List<NetworkDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> groupIds = devices.stream()
                .map(NetworkDevice::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = devices.stream()
                .map(NetworkDevice::getResponsibleUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> groupNames = groupIds.isEmpty() ? Collections.emptyMap()
                : deviceGroupMapper.selectBatchIds(groupIds).stream()
                .collect(Collectors.toMap(DeviceGroup::getId, DeviceGroup::getGroupName, (a, b) -> a));

        Map<Long, String> userNames = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(),
                        (a, b) -> a));

        return devices.stream()
                .map(device -> toVO(device, groupNames, userNames))
                .collect(Collectors.toList());
    }

    private NetworkDeviceVO toVO(NetworkDevice device, Map<Long, String> groupNames, Map<Long, String> userNames) {
        NetworkDeviceVO vo = new NetworkDeviceVO();
        vo.setId(device.getId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType());
        vo.setBrand(device.getBrand());
        vo.setModel(device.getModel());
        vo.setSerialNumber(device.getSerialNumber());
        vo.setPurchaseDate(device.getPurchaseDate());
        vo.setWarrantyExpireDate(device.getWarrantyExpireDate());
        vo.setInstallationLocation(device.getInstallationLocation());
        vo.setIpAddress(device.getIpAddress());
        vo.setMacAddress(device.getMacAddress());
        vo.setStatus(device.getStatus());
        vo.setStatusName(STATUS_NAMES.getOrDefault(device.getStatus(), device.getStatus()));
        vo.setGroupId(device.getGroupId());
        vo.setGroupName(device.getGroupId() != null ? groupNames.get(device.getGroupId()) : null);
        vo.setResponsibleUserId(device.getResponsibleUserId());
        vo.setResponsibleUserName(device.getResponsibleUserId() != null ? userNames.get(device.getResponsibleUserId()) : null);
        vo.setSupplier(device.getSupplier());
        vo.setDepartment(device.getDepartment());
        vo.setRemark(device.getRemark());
        vo.setCreatedByName(device.getCreatedBy());
        vo.setCreatedAt(device.getCreatedAt());
        vo.setUpdatedAt(device.getUpdatedAt());
        return vo;
    }

    @Override
    public NetworkDevice getDeviceById(Long id) {
        NetworkDevice device = networkDeviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return device;
    }

    @Override
    public NetworkDeviceVO getDeviceVOById(Long id) {
        NetworkDevice device = getDeviceById(id);
        NetworkDeviceVO vo = enrich(Collections.singletonList(device)).get(0);

        Long totalRepairs = repairWorkOrderMapper.selectCount(
                new LambdaQueryWrapper<RepairWorkOrder>()
                        .eq(RepairWorkOrder::getDeviceId, id));
        vo.setTotalRepairs(totalRepairs != null ? totalRepairs.intValue() : 0);

        Long completedRepairs = repairWorkOrderMapper.selectCount(
                new LambdaQueryWrapper<RepairWorkOrder>()
                        .eq(RepairWorkOrder::getDeviceId, id)
                        .eq(RepairWorkOrder::getStatus, "ACCEPTED"));
        vo.setCompletedRepairs(completedRepairs != null ? completedRepairs.intValue() : 0);

        return vo;
    }

    @Override
    @Transactional
    public NetworkDevice createDevice(NetworkDeviceDTO dto) {
        if (StringUtils.hasText(dto.getSerialNumber())) {
            NetworkDevice existing = networkDeviceMapper.selectBySerialNumber(dto.getSerialNumber());
            if (existing != null) {
                throw new BusinessException(ResultCode.DATA_EXIST.getCode(), "序列号已存在");
            }
        }
        if (StringUtils.hasText(dto.getIpAddress())) {
            NetworkDevice existing = networkDeviceMapper.selectByIpAddress(dto.getIpAddress());
            if (existing != null) {
                throw new BusinessException(ResultCode.DATA_EXIST.getCode(), "IP地址已存在");
            }
        }
        if (StringUtils.hasText(dto.getMacAddress())) {
            NetworkDevice existing = networkDeviceMapper.selectByMacAddress(dto.getMacAddress());
            if (existing != null) {
                throw new BusinessException(ResultCode.DATA_EXIST.getCode(), "MAC地址已存在");
            }
        }

        NetworkDevice device = new NetworkDevice();
        device.setDeviceName(dto.getDeviceName());
        device.setDeviceType(dto.getDeviceType());
        device.setBrand(dto.getBrand());
        device.setModel(dto.getModel());
        device.setSerialNumber(dto.getSerialNumber());
        device.setPurchaseDate(dto.getPurchaseDate());
        device.setWarrantyExpireDate(dto.getWarrantyExpireDate());
        device.setInstallationLocation(dto.getInstallationLocation());
        device.setIpAddress(dto.getIpAddress());
        device.setMacAddress(dto.getMacAddress());
        // 初始状态：前端未指定时用 NORMAL。取值已由 NetworkDeviceDTO 的 @Pattern 白名单校验，
        // 这里只做空值兜底——原先无条件写死 NORMAL，导致表单里选的状态被静默丢弃。
        device.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "NORMAL");
        // 防御 0：前端约定 0=「清空」哨兵（仅编辑场景），创建时 0 不是合法 id，一律按未选择(NULL)处理
        device.setGroupId(dto.getGroupId() != null && dto.getGroupId() != 0L ? dto.getGroupId() : null);
        device.setResponsibleUserId(dto.getResponsibleUserId() != null && dto.getResponsibleUserId() != 0L
                ? dto.getResponsibleUserId() : null);
        device.setSupplier(dto.getSupplier());
        device.setDepartment(dto.getDepartment());
        device.setRemark(dto.getRemark());

        // 设备编码取号（读 MAX + 1）与插入之间存在竞态：并发创建 / 批量导入时
        // 可能生成相同编码，后插入者撞 uk_device_code 唯一键。
        // MySQL 的重复键错误只回滚该条 INSERT，不影响当前事务，可捕获后重取序号重试。
        DuplicateKeyException conflict = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            device.setDeviceCode(generateDeviceCode());
            try {
                networkDeviceMapper.insert(device);
                conflict = null;
                break;
            } catch (DuplicateKeyException e) {
                conflict = e;
            }
        }
        if (conflict != null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "设备编码生成冲突，请稍后重试");
        }
        return device;
    }

    @Override
    @Transactional
    public NetworkDevice updateDevice(Long id, NetworkDeviceDTO dto) {
        NetworkDevice device = getDeviceById(id);

        if (StringUtils.hasText(dto.getSerialNumber()) && !dto.getSerialNumber().equals(device.getSerialNumber())) {
            NetworkDevice existing = networkDeviceMapper.selectBySerialNumber(dto.getSerialNumber());
            if (existing != null) {
                throw new BusinessException(ResultCode.DATA_EXIST.getCode(), "序列号已存在");
            }
        }
        if (StringUtils.hasText(dto.getIpAddress()) && !dto.getIpAddress().equals(device.getIpAddress())) {
            NetworkDevice existing = networkDeviceMapper.selectByIpAddress(dto.getIpAddress());
            if (existing != null) {
                throw new BusinessException(ResultCode.DATA_EXIST.getCode(), "IP地址已存在");
            }
        }
        if (StringUtils.hasText(dto.getMacAddress()) && !dto.getMacAddress().equals(device.getMacAddress())) {
            NetworkDevice existing = networkDeviceMapper.selectByMacAddress(dto.getMacAddress());
            if (existing != null) {
                throw new BusinessException(ResultCode.DATA_EXIST.getCode(), "MAC地址已存在");
            }
        }

        if (StringUtils.hasText(dto.getDeviceName())) device.setDeviceName(dto.getDeviceName());
        if (StringUtils.hasText(dto.getDeviceType())) device.setDeviceType(dto.getDeviceType());
        if (StringUtils.hasText(dto.getBrand())) device.setBrand(dto.getBrand());
        if (StringUtils.hasText(dto.getModel())) device.setModel(dto.getModel());
        if (StringUtils.hasText(dto.getSerialNumber())) device.setSerialNumber(dto.getSerialNumber());
        if (dto.getPurchaseDate() != null) device.setPurchaseDate(dto.getPurchaseDate());
        if (dto.getWarrantyExpireDate() != null) device.setWarrantyExpireDate(dto.getWarrantyExpireDate());
        if (StringUtils.hasText(dto.getInstallationLocation())) device.setInstallationLocation(dto.getInstallationLocation());
        if (StringUtils.hasText(dto.getIpAddress())) device.setIpAddress(dto.getIpAddress());
        if (StringUtils.hasText(dto.getMacAddress())) device.setMacAddress(dto.getMacAddress());
        // 0 是前端「清空」哨兵：el-tree-select / el-select 清空后值是 undefined，JSON 序列化
        // 会直接省略字段，后端无法区分「没传」与「清空」；分组/用户 id 自增从 1 开始，
        // 0 不是合法 id，用它表达清空意图。置 NULL 必须走 UpdateWrapper——
        // updateById 的 NOT_NULL 策略会跳过 null 字段，实体 set null 无效。
        boolean clearGroup = dto.getGroupId() != null && dto.getGroupId() == 0L;
        boolean clearResponsible = dto.getResponsibleUserId() != null && dto.getResponsibleUserId() == 0L;
        if (dto.getGroupId() != null && !clearGroup) device.setGroupId(dto.getGroupId());
        if (dto.getResponsibleUserId() != null && !clearResponsible) device.setResponsibleUserId(dto.getResponsibleUserId());
        if (StringUtils.hasText(dto.getSupplier())) device.setSupplier(dto.getSupplier());
        if (StringUtils.hasText(dto.getDepartment())) device.setDepartment(dto.getDepartment());
        if (dto.getRemark() != null) device.setRemark(dto.getRemark());

        networkDeviceMapper.updateById(device);
        if (clearGroup || clearResponsible) {
            // 显式 SET NULL 清掉关联；update(null, wrapper) 只更新 wrapper 中声明的字段
            LambdaUpdateWrapper<NetworkDevice> clearWrapper = new LambdaUpdateWrapper<NetworkDevice>()
                    .eq(NetworkDevice::getId, id)
                    .set(clearGroup, NetworkDevice::getGroupId, null)
                    .set(clearResponsible, NetworkDevice::getResponsibleUserId, null);
            networkDeviceMapper.update(null, clearWrapper);
        }
        return device;
    }

    @Override
    @Transactional
    public void deleteDevice(Long id) {
        getDeviceById(id);
        networkDeviceMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status, String remark) {
        NetworkDevice device = getDeviceById(id);
        String originalStatus = device.getStatus();
        device.setStatus(status);
        networkDeviceMapper.updateById(device);

        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(id);
        log.setDeviceCode(device.getDeviceCode());
        log.setOriginalStatus(originalStatus);
        log.setNewStatus(status);
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        deviceStatusLogMapper.insert(log);
    }

    @Override
    @Transactional
    public void batchUpdateGroup(List<Long> deviceIds, Long groupId) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        for (Long deviceId : deviceIds) {
            NetworkDevice device = networkDeviceMapper.selectById(deviceId);
            if (device != null) {
                device.setGroupId(groupId);
                networkDeviceMapper.updateById(device);
            }
        }
    }

    @Override
    public void importDevices(List<NetworkDeviceDTO> devices) {
        if (devices == null) {
            return;
        }
        for (NetworkDeviceDTO dto : devices) {
            createDevice(dto);
        }
    }

    /**
     * 解析 Excel 并逐行导入。
     * 单行失败不影响其它行，全部失败时抛出异常并把失败原因返回给前端。
     */
    @Override
    public int importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上传文件为空");
        }

        List<DeviceExcelDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(DeviceExcelDTO.class)
                    .sheet()
                    .headRowNumber(1)
                    .doReadSync();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Excel 解析失败：" + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Excel 格式不正确：" + e.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Excel 中没有可导入的数据");
        }

        int success = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            DeviceExcelDTO row = rows.get(i);
            int rowNo = i + 2;
            if (!StringUtils.hasText(row.getDeviceName())) {
                errors.add("第" + rowNo + "行：设备名称不能为空");
                continue;
            }
            try {
                NetworkDeviceDTO dto = new NetworkDeviceDTO();
                dto.setDeviceName(row.getDeviceName());
                dto.setDeviceType(row.getDeviceType());
                dto.setBrand(row.getBrand());
                dto.setModel(row.getModel());
                dto.setSerialNumber(row.getSerialNumber());
                dto.setPurchaseDate(parseDate(row.getPurchaseDate()));
                dto.setWarrantyExpireDate(parseDate(row.getWarrantyExpireDate()));
                dto.setInstallationLocation(row.getInstallationLocation());
                dto.setIpAddress(row.getIpAddress());
                dto.setMacAddress(row.getMacAddress());
                dto.setSupplier(row.getSupplier());
                dto.setDepartment(row.getDepartment());
                dto.setRemark(row.getRemark());

                createDevice(dto);
                success++;
            } catch (Exception e) {
                errors.add("第" + rowNo + "行：" + e.getMessage());
            }
        }

        if (success == 0) {
            String detail = errors.stream().limit(5).collect(Collectors.joining("；"));
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "导入失败：" + detail);
        }
        return success;
    }

    @Override
    public List<DeviceExcelDTO> exportExcelData(DeviceQueryDTO queryDTO) {
        DeviceQueryDTO exportQuery = new DeviceQueryDTO();
        exportQuery.setPage(1);
        exportQuery.setPageSize(EXPORT_MAX_ROWS);
        exportQuery.setDeviceCode(queryDTO.getDeviceCode());
        exportQuery.setDeviceName(queryDTO.getDeviceName());
        exportQuery.setDeviceType(queryDTO.getDeviceType());
        exportQuery.setBrand(queryDTO.getBrand());
        exportQuery.setSerialNumber(queryDTO.getSerialNumber());
        exportQuery.setIpAddress(queryDTO.getIpAddress());
        exportQuery.setMacAddress(queryDTO.getMacAddress());
        exportQuery.setGroupId(queryDTO.getGroupId());
        exportQuery.setResponsibleUserId(queryDTO.getResponsibleUserId());
        exportQuery.setStatus(queryDTO.getStatus());
        exportQuery.setInstallationLocation(queryDTO.getInstallationLocation());
        exportQuery.setPurchaseDateStart(queryDTO.getPurchaseDateStart());
        exportQuery.setPurchaseDateEnd(queryDTO.getPurchaseDateEnd());
        exportQuery.setWarrantyExpireDateStart(queryDTO.getWarrantyExpireDateStart());
        exportQuery.setWarrantyExpireDateEnd(queryDTO.getWarrantyExpireDateEnd());

        return listDevices(exportQuery).getRecords().stream()
                .map(this::toExcelDTO)
                .collect(Collectors.toList());
    }

    private DeviceExcelDTO toExcelDTO(NetworkDeviceVO vo) {
        DeviceExcelDTO dto = new DeviceExcelDTO();
        dto.setDeviceCode(vo.getDeviceCode());
        dto.setDeviceName(vo.getDeviceName());
        dto.setDeviceType(vo.getDeviceType());
        dto.setBrand(vo.getBrand());
        dto.setModel(vo.getModel());
        dto.setSerialNumber(vo.getSerialNumber());
        dto.setPurchaseDate(vo.getPurchaseDate() != null ? vo.getPurchaseDate().toString() : "");
        dto.setWarrantyExpireDate(vo.getWarrantyExpireDate() != null ? vo.getWarrantyExpireDate().toString() : "");
        dto.setInstallationLocation(vo.getInstallationLocation());
        dto.setIpAddress(vo.getIpAddress());
        dto.setMacAddress(vo.getMacAddress());
        dto.setStatus(vo.getStatusName());
        dto.setGroupName(vo.getGroupName());
        dto.setResponsibleUserName(vo.getResponsibleUserName());
        dto.setSupplier(vo.getSupplier());
        dto.setDepartment(vo.getDepartment());
        dto.setRemark(vo.getRemark());
        return dto;
    }

    /** 解析 yyyy-M-d / yyyy/M/d 文本或 Excel 日期序列号 */
    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.contains("-") || text.contains("/")) {
                return LocalDate.parse(text.replace('/', '-'), DATE_FORMAT);
            }
            return LocalDate.of(1899, 12, 30).plusDays(Long.parseLong(text));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "日期格式不正确：" + value);
        }
    }

    private String generateDeviceCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer maxSeq = networkDeviceMapper.getMaxDeviceCodeOfDay();
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
        return String.format("NET-%s-%06d", dateStr, nextSeq);
    }
}
