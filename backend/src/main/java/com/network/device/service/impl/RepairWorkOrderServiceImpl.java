package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.dto.*;
import com.network.device.entity.*;
import com.network.device.enums.RepairStatus;
import com.network.device.mapper.*;
import com.network.device.security.LoginUser;
import com.network.device.service.FileStorageService;
import com.network.device.service.NotificationService;
import com.network.device.service.RepairWorkOrderService;
import com.network.device.vo.AttachmentVO;
import com.network.device.vo.OrderLogVO;
import com.network.device.vo.RepairHardwareDetailVO;
import com.network.device.vo.RepairDebugDetailVO;
import com.network.device.vo.RepairOpticalDetailVO;
import com.network.device.vo.RepairWorkOrderVO;
import com.network.device.vo.UploadResultVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RepairWorkOrderServiceImpl implements RepairWorkOrderService {

    private static final Logger logger = LoggerFactory.getLogger(RepairWorkOrderServiceImpl.class);

    private final RepairWorkOrderMapper repairWorkOrderMapper;
    private final RepairHardwareDetailMapper repairHardwareDetailMapper;
    private final RepairDebugDetailMapper repairDebugDetailMapper;
    private final RepairOpticalDetailMapper repairOpticalDetailMapper;
    private final RepairStatusLogMapper repairStatusLogMapper;
    private final NetworkDeviceMapper networkDeviceMapper;
    private final SysUserMapper sysUserMapper;
    private final RepairAttachmentMapper repairAttachmentMapper;
    private final FileStorageService fileStorageService;
    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final NotificationService notificationService;

    public RepairWorkOrderServiceImpl(RepairWorkOrderMapper repairWorkOrderMapper,
                                      RepairHardwareDetailMapper repairHardwareDetailMapper,
                                      RepairDebugDetailMapper repairDebugDetailMapper,
                                      RepairOpticalDetailMapper repairOpticalDetailMapper,
                                      RepairStatusLogMapper repairStatusLogMapper,
                                      NetworkDeviceMapper networkDeviceMapper,
                                      SysUserMapper sysUserMapper,
                                      RepairAttachmentMapper repairAttachmentMapper,
                                      FileStorageService fileStorageService,
                                      DeviceStatusLogMapper deviceStatusLogMapper,
                                      NotificationService notificationService) {
        this.repairWorkOrderMapper = repairWorkOrderMapper;
        this.repairHardwareDetailMapper = repairHardwareDetailMapper;
        this.repairDebugDetailMapper = repairDebugDetailMapper;
        this.repairOpticalDetailMapper = repairOpticalDetailMapper;
        this.repairStatusLogMapper = repairStatusLogMapper;
        this.networkDeviceMapper = networkDeviceMapper;
        this.sysUserMapper = sysUserMapper;
        this.repairAttachmentMapper = repairAttachmentMapper;
        this.fileStorageService = fileStorageService;
        this.deviceStatusLogMapper = deviceStatusLogMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Page<RepairWorkOrderVO> listOrders(RepairOrderQueryDTO queryDTO) {
        Page<RepairWorkOrder> entityPage = queryOrders(queryDTO);
        List<RepairWorkOrderVO> vos = convertToVOList(entityPage.getRecords());

        Page<RepairWorkOrderVO> voPage =
                new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    /** 分页查询工单实体（内部复用，导出也走这里） */
    private Page<RepairWorkOrder> queryOrders(RepairOrderQueryDTO queryDTO) {
        Page<RepairWorkOrder> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        LambdaQueryWrapper<RepairWorkOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getWorkOrderNo())) {
            wrapper.like(RepairWorkOrder::getWorkOrderNo, queryDTO.getWorkOrderNo());
        }
        if (StringUtils.hasText(queryDTO.getDeviceCode())) {
            wrapper.like(RepairWorkOrder::getDeviceCode, queryDTO.getDeviceCode());
        }
        if (StringUtils.hasText(queryDTO.getDeviceName())) {
            wrapper.like(RepairWorkOrder::getDeviceName, queryDTO.getDeviceName());
        }
        if (StringUtils.hasText(queryDTO.getRepairType())) {
            wrapper.eq(RepairWorkOrder::getRepairType, queryDTO.getRepairType());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(RepairWorkOrder::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getRepairUserId() != null) {
            wrapper.eq(RepairWorkOrder::getRepairUserId, queryDTO.getRepairUserId());
        }
        if (queryDTO.getReporterId() != null) {
            wrapper.eq(RepairWorkOrder::getReporterId, queryDTO.getReporterId());
        }
        if (queryDTO.getGroupId() != null) {
            wrapper.eq(RepairWorkOrder::getGroupId, queryDTO.getGroupId());
        }
        if (StringUtils.hasText(queryDTO.getFaultTimeStart())) {
            wrapper.ge(RepairWorkOrder::getFaultTime, parseTimeBoundary(queryDTO.getFaultTimeStart()));
        }
        if (StringUtils.hasText(queryDTO.getFaultTimeEnd())) {
            // 上界取「次日 0 点」的左闭右开区间：前端日期选择器只给到 yyyy-MM-dd，
            // 若直接 <= 该日 00:00:00，结束当天的一整天数据都会被排除掉。
            String end = queryDTO.getFaultTimeEnd().trim();
            LocalDateTime endExclusive = parseTimeBoundary(end);
            if (end.length() <= 10) {
                endExclusive = endExclusive.plusDays(1);
            }
            wrapper.lt(RepairWorkOrder::getFaultTime, endExclusive);
        }
        if (StringUtils.hasText(queryDTO.getPriority())) {
            wrapper.eq(RepairWorkOrder::getPriority, queryDTO.getPriority());
        }

        wrapper.orderByDesc(RepairWorkOrder::getCreatedAt);
        return repairWorkOrderMapper.selectPage(page, wrapper);
    }

    /**
     * 解析前端传来的时间边界。
     *
     * <p>前端日期选择器的 value-format 是 {@code YYYY-MM-DD}（只到天），而这里原先直接调用
     * {@code LocalDateTime.parse(...)}——{@code ISO_LOCAL_DATE_TIME} 要求必须带时间部分，
     * 解析 {@code "2026-09-01"} 会抛 {@code DateTimeParseException}，使「按时间范围筛选/导出」
     * 直接变成 500。这里同时兼容纯日期与日期时间两种写法。
     */
    private LocalDateTime parseTimeBoundary(String value) {
        String v = value.trim();
        if (v.length() <= 10) {
            return LocalDate.parse(v).atStartOfDay();
        }
        // 兼容 "yyyy-MM-dd HH:mm:ss"（前端常用）与 ISO 的 "yyyy-MM-ddTHH:mm:ss"
        return LocalDateTime.parse(v.replace(' ', 'T'));
    }

    @Override
    public RepairWorkOrder getOrderById(Long id) {
        RepairWorkOrder order = repairWorkOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return order;
    }

    @Override
    public RepairWorkOrderVO getOrderVOById(Long id) {
        RepairWorkOrder order = getOrderById(id);
        RepairWorkOrderVO vo = convertToVOList(Collections.singletonList(order)).get(0);

        RepairHardwareDetail hardwareDetail = repairHardwareDetailMapper.selectByWorkOrderId(id);
        if (hardwareDetail != null) {
            RepairHardwareDetailVO hvo = new RepairHardwareDetailVO();
            hvo.setId(hardwareDetail.getId());
            hvo.setWorkOrderId(hardwareDetail.getWorkOrderId());
            hvo.setDamagedComponent(hardwareDetail.getDamagedComponent());
            hvo.setReplacementPartName(hardwareDetail.getReplacementPartName());
            hvo.setReplacementPartModel(hardwareDetail.getReplacementPartModel());
            hvo.setReplacementPartQuantity(hardwareDetail.getReplacementPartQuantity());
            hvo.setReplacementPartCost(hardwareDetail.getReplacementPartCost());
            hvo.setOldPartDisposalMethod(hardwareDetail.getOldPartDisposalMethod());
            hvo.setHardwareFailureCode(hardwareDetail.getHardwareFailureCode());
            hvo.setWhetherUnderWarranty(hardwareDetail.getWhetherUnderWarranty());
            hvo.setSupplier(hardwareDetail.getSupplier());
            hvo.setRemark(hardwareDetail.getRemark());
            vo.setHardwareDetail(hvo);
        }

        RepairDebugDetail debugDetail = repairDebugDetailMapper.selectByWorkOrderId(id);
        if (debugDetail != null) {
            RepairDebugDetailVO dvo = new RepairDebugDetailVO();
            dvo.setId(debugDetail.getId());
            dvo.setWorkOrderId(debugDetail.getWorkOrderId());
            dvo.setConfigurationChangeContent(debugDetail.getConfigurationChangeContent());
            dvo.setOldFirmwareVersion(debugDetail.getOldFirmwareVersion());
            dvo.setNewFirmwareVersion(debugDetail.getNewFirmwareVersion());
            dvo.setOldIpAddress(debugDetail.getOldIpAddress());
            dvo.setNewIpAddress(debugDetail.getNewIpAddress());
            dvo.setOldGateway(debugDetail.getOldGateway());
            dvo.setNewGateway(debugDetail.getNewGateway());
            dvo.setOldVlan(debugDetail.getOldVlan());
            dvo.setNewVlan(debugDetail.getNewVlan());
            dvo.setRouteChangeContent(debugDetail.getRouteChangeContent());
            dvo.setFirewallPolicyChange(debugDetail.getFirewallPolicyChange());
            dvo.setPermissionChangeContent(debugDetail.getPermissionChangeContent());
            dvo.setNetworkParameterChangeRecord(debugDetail.getNetworkParameterChangeRecord());
            dvo.setRollbackPlan(debugDetail.getRollbackPlan());
            dvo.setTestResult(debugDetail.getTestResult());
            dvo.setRemark(debugDetail.getRemark());
            vo.setDebugDetail(dvo);
        }

        RepairOpticalDetail opticalDetail = repairOpticalDetailMapper.selectByWorkOrderId(id);
        if (opticalDetail != null) {
            RepairOpticalDetailVO ovo = new RepairOpticalDetailVO();
            ovo.setId(opticalDetail.getId());
            ovo.setWorkOrderId(opticalDetail.getWorkOrderId());
            ovo.setFaultOpticalPoint(opticalDetail.getFaultOpticalPoint());
            ovo.setOpticalRouteName(opticalDetail.getOpticalRouteName());
            ovo.setCableSection(opticalDetail.getCableSection());
            ovo.setCableLength(opticalDetail.getCableLength());
            ovo.setOpticalPowerBefore(opticalDetail.getOpticalPowerBefore());
            ovo.setOpticalPowerAfter(opticalDetail.getOpticalPowerAfter());
            ovo.setAttenuationBefore(opticalDetail.getAttenuationBefore());
            ovo.setAttenuationAfter(opticalDetail.getAttenuationAfter());
            ovo.setWavelength(opticalDetail.getWavelength());
            ovo.setSplitterStatus(opticalDetail.getSplitterStatus());
            ovo.setJumperStatus(opticalDetail.getJumperStatus());
            ovo.setCableDamageDescription(opticalDetail.getCableDamageDescription());
            ovo.setLinkTestResult(opticalDetail.getLinkTestResult());
            ovo.setTestTool(opticalDetail.getTestTool());
            ovo.setTestPerson(opticalDetail.getTestPerson());
            ovo.setRemark(opticalDetail.getRemark());
            vo.setOpticalDetail(ovo);
        }

        // 前端 detail.vue 直接读 order.logs，这里一并返回
        List<OrderLogVO> logs = getOrderLogs(id);
        vo.setLogs(logs);

        // 工单详情要回显已上传附件：process 页进入时 uploadFiles 从 attachments 回填，
        // 不组装则已上传的文件在页面上永远不可见（文件实际已入库）。
        // listAttachments 含上传人姓名批量解析，直接复用。
        vo.setAttachments(listAttachments(id));

        // rejectReason / repairStartTime / repairEndTime 在 repair_work_order 里没有对应列，
        // 从状态流转日志与完成时间推导，避免前端这三处永远为空
        vo.setRepairEndTime(order.getCompletedTime());
        if (logs != null) {
            logs.stream()
                    .filter(l -> RepairStatus.PROCESSING.name().equals(l.getToStatus()))
                    .map(OrderLogVO::getCreateTime)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .ifPresent(vo::setRepairStartTime);
            // 退回原因取最近一次退回日志的备注（保存时形如「退回工单: xxx」，这里剥掉前缀）
            logs.stream()
                    .filter(l -> RepairStatus.REJECTED.name().equals(l.getToStatus()))
                    .reduce((first, second) -> second)
                    .map(OrderLogVO::getRemark)
                    .filter(StringUtils::hasText)
                    .map(r -> r.replaceFirst("^退回工单[:：]\\s*", ""))
                    .ifPresent(vo::setRejectReason);
        }

        return vo;
    }

    @Override
    @Transactional
    public RepairWorkOrderVO updateOrder(Long id, RepairWorkOrderDTO dto) {
        RepairWorkOrder order = getOrderById(id);

        if (dto.getDeviceId() != null) {
            order.setDeviceId(dto.getDeviceId());
        }
        if (dto.getFaultTime() != null) {
            order.setFaultTime(dto.getFaultTime());
        }
        if (StringUtils.hasText(dto.getRepairType())) {
            order.setRepairType(dto.getRepairType());
        }
        if (StringUtils.hasText(dto.getFaultDescription())) {
            order.setFaultDescription(dto.getFaultDescription());
        }
        if (StringUtils.hasText(dto.getPriority())) {
            order.setPriority(dto.getPriority());
        }
        if (dto.getRemark() != null) {
            order.setRemark(dto.getRemark());
        }
        repairWorkOrderMapper.updateById(order);

        return getOrderVOById(id);
    }

    /** 取号撞唯一键时的最大重试次数（并发创建同一毫秒窗口内多个工单的场景） */
    private static final int WORK_ORDER_NO_MAX_RETRY = 3;

    @Override
    @Transactional
    public RepairWorkOrder createOrder(RepairWorkOrderDTO dto) {
        NetworkDevice device = networkDeviceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST.getCode(), "设备不存在");
        }

        LoginUser loginUser = getCurrentLoginUser();

        RepairWorkOrder order = new RepairWorkOrder();
        order.setDeviceId(dto.getDeviceId());
        order.setDeviceCode(device.getDeviceCode());
        order.setDeviceName(device.getDeviceName());
        order.setGroupId(device.getGroupId());
        order.setFaultTime(dto.getFaultTime() != null ? dto.getFaultTime() : LocalDateTime.now());
        order.setReporterId(loginUser.getId());
        order.setRepairType(dto.getRepairType());
        order.setStatus(RepairStatus.DRAFT.name());
        order.setFaultDescription(dto.getFaultDescription());
        order.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
        order.setRemark(dto.getRemark());

        // 取号（读 MAX + 1）与插入之间存在竞态：两个并发请求会读到同一个序号，
        // 后插入者撞 uk_work_order_no 唯一键。MySQL 的重复键错误只回滚该条 INSERT、
        // 不影响当前事务，因此可以捕获后重取序号再试（最多 3 次）。
        DuplicateKeyException conflict = null;
        for (int attempt = 0; attempt < WORK_ORDER_NO_MAX_RETRY; attempt++) {
            order.setWorkOrderNo(generateWorkOrderNo());
            try {
                repairWorkOrderMapper.insert(order);
                conflict = null;
                break;
            } catch (DuplicateKeyException e) {
                conflict = e;
            }
        }
        if (conflict != null) {
            logger.error("工单号生成冲突，重试 {} 次仍失败", WORK_ORDER_NO_MAX_RETRY, conflict);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "工单号生成冲突，请稍后重试");
        }

        saveStatusLog(order.getId(), order.getWorkOrderNo(), null, RepairStatus.DRAFT.name(), loginUser.getId(), "创建工单");

        // 此前有一行 networkDeviceMapper.updateById(device)，但 device 自 selectById 以来
        // 未改任何字段——是一次无意义的 UPDATE，删掉。

        return order;
    }

    @Override
    @Transactional
    public void submitOrder(Long id) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.DRAFT.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有草稿状态的工单可以提交");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setStatus(RepairStatus.SUBMITTED.name());
        repairWorkOrderMapper.updateById(order);

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.SUBMITTED.name(), loginUser.getId(), "提交工单");
    }

    @Override
    @Transactional
    public void assignOrder(Long id, AssignDTO dto) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.SUBMITTED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有已提交状态的工单可以分配");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setRepairUserId(dto.getRepairUserId());
        order.setPlanCompleteTime(dto.getPlanCompleteTime());
        if (StringUtils.hasText(dto.getPriority())) {
            order.setPriority(dto.getPriority());
        }
        order.setStatus(RepairStatus.ASSIGNED.name());
        repairWorkOrderMapper.updateById(order);

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.ASSIGNED.name(), loginUser.getId(), "分配工单");

        NetworkDevice device = networkDeviceMapper.selectById(order.getDeviceId());
        changeDeviceStatus(device, "FAULT_REPAIR",
                "工单 " + order.getWorkOrderNo() + " 已分配，设备转入故障维修");

        notifyAfterCommit(order.getRepairUserId(), loginUser.getId(), order,
                "有新工单指派给你：" + order.getWorkOrderNo(),
                "设备「" + order.getDeviceName() + "」的维修工单已指派给你，请及时处理。");
    }

    @Override
    @Transactional
    public void startProcessing(Long id) {
        RepairWorkOrder order = getOrderById(id);
        // 已退回的工单同样需要重新进入处理流程（验收退回后无其他出口，
        // 若只放行 ASSIGNED，REJECTED 会成为无法流转的死状态）
        if (!RepairStatus.ASSIGNED.name().equals(order.getStatus())
                && !RepairStatus.REJECTED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有已分配或已退回状态的工单可以开始处理");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setStatus(RepairStatus.PROCESSING.name());
        repairWorkOrderMapper.updateById(order);

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.PROCESSING.name(), loginUser.getId(),
                RepairStatus.REJECTED.name().equals(originalStatus) ? "退回后重新开始处理" : "开始处理");
    }

    @Override
    @Transactional
    public void completeOrder(Long id, CompleteDTO dto) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.PROCESSING.name().equals(order.getStatus())
                && !RepairStatus.DELAYED.name().equals(order.getStatus())
                && !RepairStatus.REPAIR_AGAIN.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "当前状态不允许完成工单");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setCompletedTime(LocalDateTime.now());
        order.setRepairSolution(dto.getRepairSolution());
        order.setRepairResult(dto.getRepairResult());

        if (order.getFaultTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(order.getFaultTime(), LocalDateTime.now());
            order.setRepairDuration(minutes);
        }

        if (order.getPlanCompleteTime() != null && LocalDateTime.now().isAfter(order.getPlanCompleteTime())) {
            order.setOverdue(1);
        }

        // 设备维修后状态：必须在 updateById 之前设置，否则写入 DB 时该字段为 null，
        // 重新从 DB 加载后会丢失（之前 setDeviceRepairStatus 在 updateById 之后执行，
        // 只改了内存对象、从未持久化）。
        if (StringUtils.hasText(dto.getRepairResult())) {
            order.setDeviceRepairStatus(dto.getRepairResult());
        }

        order.setStatus(RepairStatus.COMPLETED.name());
        repairWorkOrderMapper.updateById(order);

        if (dto.getHardwareDetail() != null) {
            saveHardwareDetail(id, dto.getHardwareDetail());
        }
        if (dto.getDebugDetail() != null) {
            saveDebugDetail(id, dto.getDebugDetail());
        }
        if (dto.getOpticalDetail() != null) {
            saveOpticalDetail(id, dto.getOpticalDetail());
        }

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.COMPLETED.name(), loginUser.getId(), "完成维修");

        notifyAfterCommit(order.getReporterId(), loginUser.getId(), order,
                "工单已完成，待验收：" + order.getWorkOrderNo(),
                "设备「" + order.getDeviceName() + "」的维修工单已完成，请及时验收。");
    }

    @Override
    @Transactional
    public void acceptOrder(Long id, AcceptDTO dto) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有已完成状态的工单可以验收");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setAcceptanceUserId(loginUser.getId());
        order.setAcceptanceTime(LocalDateTime.now());
        order.setStatus(RepairStatus.ACCEPTED.name());
        repairWorkOrderMapper.updateById(order);

        // 前端「验收备注」是可选字段：填了要落到日志里，没填保持原文案。
        String remark = dto == null ? null : dto.getRemark();
        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.ACCEPTED.name(),
                loginUser.getId(),
                StringUtils.hasText(remark) ? "验收通过: " + remark : "验收通过");

        NetworkDevice device = networkDeviceMapper.selectById(order.getDeviceId());
        changeDeviceStatus(device, "NORMAL",
                "工单 " + order.getWorkOrderNo() + " 验收通过，设备恢复正常");

        notifyAfterCommit(order.getRepairUserId(), loginUser.getId(), order,
                "工单已验收：" + order.getWorkOrderNo(),
                "设备「" + order.getDeviceName() + "」的维修工单已验收通过。");
    }

    @Override
    @Transactional
    public void rejectOrder(Long id, RejectDTO dto) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有已完成状态的工单可以退回");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setStatus(RepairStatus.REJECTED.name());
        order.setRemark(dto.getReason());
        repairWorkOrderMapper.updateById(order);

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.REJECTED.name(), loginUser.getId(), "退回工单: " + dto.getReason());

        notifyAfterCommit(order.getRepairUserId(), loginUser.getId(), order,
                "工单被退回：" + order.getWorkOrderNo(),
                "设备「" + order.getDeviceName() + "」的维修工单被退回，原因：" + dto.getReason());
    }

    @Override
    @Transactional
    public void delayOrder(Long id, DelayDTO dto) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.PROCESSING.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有处理中状态的工单可以延期");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setStatus(RepairStatus.DELAYED.name());
        order.setPlanCompleteTime(dto.getPlanCompleteTime());
        order.setRemark(dto.getReason());
        repairWorkOrderMapper.updateById(order);

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.DELAYED.name(), loginUser.getId(), "延期: " + dto.getReason());

        notifyAfterCommit(order.getReporterId(), loginUser.getId(), order,
                "工单已延期：" + order.getWorkOrderNo(),
                "设备「" + order.getDeviceName() + "」的维修工单已延期，原因：" + dto.getReason());
    }

    @Override
    @Transactional
    public void repairAgain(Long id) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.CLOSED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有已关闭状态的工单可以返修");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setStatus(RepairStatus.REPAIR_AGAIN.name());
        repairWorkOrderMapper.updateById(order);
        // 返修要清空上一轮的完成痕迹（完成时间/维修方案/维修结果），但 updateById 的
        // NOT_NULL 策略会跳过 null 字段，实体 set null 无效——必须用 UpdateWrapper
        // 显式 SET NULL，否则返修中的工单仍显示上一轮的方案/结果/完成时间。
        repairWorkOrderMapper.update(null, new LambdaUpdateWrapper<RepairWorkOrder>()
                .eq(RepairWorkOrder::getId, id)
                .set(RepairWorkOrder::getCompletedTime, null)
                .set(RepairWorkOrder::getRepairSolution, null)
                .set(RepairWorkOrder::getRepairResult, null));

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.REPAIR_AGAIN.name(), loginUser.getId(), "返修");

        NetworkDevice device = networkDeviceMapper.selectById(order.getDeviceId());
        changeDeviceStatus(device, "FAULT_REPAIR",
                "工单 " + order.getWorkOrderNo() + " 发起返修，设备转回故障维修");

        notifyAfterCommit(order.getRepairUserId(), loginUser.getId(), order,
                "工单已发起返修：" + order.getWorkOrderNo(),
                "设备「" + order.getDeviceName() + "」的维修工单已发起返修，请重新处理。");
    }

    @Override
    @Transactional
    public void closeOrder(Long id) {
        RepairWorkOrder order = getOrderById(id);
        if (!RepairStatus.ACCEPTED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "只有已验收状态的工单可以关闭");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setStatus(RepairStatus.CLOSED.name());
        repairWorkOrderMapper.updateById(order);

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.CLOSED.name(), loginUser.getId(), "关闭工单");
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        RepairWorkOrder order = getOrderById(id);
        if (RepairStatus.CANCELLED.name().equals(order.getStatus())
                || RepairStatus.ACCEPTED.name().equals(order.getStatus())
                || RepairStatus.CLOSED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "当前状态不允许取消工单");
        }

        LoginUser loginUser = getCurrentLoginUser();
        String originalStatus = order.getStatus();
        order.setStatus(RepairStatus.CANCELLED.name());
        repairWorkOrderMapper.updateById(order);

        saveStatusLog(order.getId(), order.getWorkOrderNo(), originalStatus, RepairStatus.CANCELLED.name(), loginUser.getId(), "取消工单");

        // 分配 / 返修会把设备置为 FAULT_REPAIR，取消工单时若不恢复，
        // 设备会永远卡在「故障维修」状态（除非手工改状态或再来一张工单）。
        // 只有该设备**没有其他活跃工单**时才恢复 NORMAL——
        // COMPLETED（待验收）也算活跃：它验收时会把设备恢复正常，这里恢复会重复。
        restoreDeviceIfNoActiveOrder(order);

        notifyAfterCommit(order.getRepairUserId(), loginUser.getId(), order,
                "工单已取消：" + order.getWorkOrderNo(),
                "设备「" + order.getDeviceName() + "」的维修工单已取消。");
    }

    /**
     * 取消工单后，若该设备已无其他活跃工单，则把设备从「故障维修」恢复为「正常」。
     *
     * <p>活跃工单 = 未进入终态（ACCEPTED / CLOSED / CANCELLED）的工单。
     * 逻辑删除的行由 @TableLogic 自动排除，不会误算。
     * {@link #changeDeviceStatus} 内部会在状态未变时跳过，并写入设备状态日志。
     */
    private void restoreDeviceIfNoActiveOrder(RepairWorkOrder cancelledOrder) {
        Long activeCount = repairWorkOrderMapper.selectCount(new LambdaQueryWrapper<RepairWorkOrder>()
                .eq(RepairWorkOrder::getDeviceId, cancelledOrder.getDeviceId())
                .ne(RepairWorkOrder::getId, cancelledOrder.getId())
                .notIn(RepairWorkOrder::getStatus,
                        RepairStatus.ACCEPTED.name(),
                        RepairStatus.CLOSED.name(),
                        RepairStatus.CANCELLED.name()));
        if (activeCount != null && activeCount > 0) {
            return;
        }
        NetworkDevice device = networkDeviceMapper.selectById(cancelledOrder.getDeviceId());
        changeDeviceStatus(device, "NORMAL",
                "工单 " + cancelledOrder.getWorkOrderNo() + " 已取消，设备恢复正常");
    }

    @Override
    @Transactional
    public void saveHardwareDetail(Long workOrderId, CompleteDTO.HardwareDetailDTO detail) {
        RepairHardwareDetail hardwareDetail = repairHardwareDetailMapper.selectByWorkOrderId(workOrderId);
        if (hardwareDetail == null) {
            hardwareDetail = new RepairHardwareDetail();
            hardwareDetail.setWorkOrderId(workOrderId);
        }

        hardwareDetail.setDamagedComponent(detail.getDamagedComponent());
        hardwareDetail.setReplacementPartName(detail.getReplacementPartName());
        hardwareDetail.setReplacementPartModel(detail.getReplacementPartModel());
        hardwareDetail.setReplacementPartQuantity(detail.getReplacementPartQuantity());
        hardwareDetail.setReplacementPartCost(detail.getReplacementPartCost());
        hardwareDetail.setOldPartDisposalMethod(detail.getOldPartDisposalMethod());
        hardwareDetail.setHardwareFailureCode(detail.getHardwareFailureCode());
        hardwareDetail.setWhetherUnderWarranty(detail.getWhetherUnderWarranty());
        hardwareDetail.setSupplier(detail.getSupplier());
        hardwareDetail.setRemark(detail.getRemark());

        if (hardwareDetail.getId() == null) {
            repairHardwareDetailMapper.insert(hardwareDetail);
        } else {
            repairHardwareDetailMapper.updateById(hardwareDetail);
        }
    }

    @Override
    @Transactional
    public void saveDebugDetail(Long workOrderId, CompleteDTO.DebugDetailDTO detail) {
        RepairDebugDetail debugDetail = repairDebugDetailMapper.selectByWorkOrderId(workOrderId);
        if (debugDetail == null) {
            debugDetail = new RepairDebugDetail();
            debugDetail.setWorkOrderId(workOrderId);
        }

        debugDetail.setConfigurationChangeContent(detail.getConfigurationChangeContent());
        debugDetail.setOldFirmwareVersion(detail.getOldFirmwareVersion());
        debugDetail.setNewFirmwareVersion(detail.getNewFirmwareVersion());
        debugDetail.setOldIpAddress(detail.getOldIpAddress());
        debugDetail.setNewIpAddress(detail.getNewIpAddress());
        debugDetail.setOldGateway(detail.getOldGateway());
        debugDetail.setNewGateway(detail.getNewGateway());
        debugDetail.setOldVlan(detail.getOldVlan());
        debugDetail.setNewVlan(detail.getNewVlan());
        debugDetail.setRouteChangeContent(detail.getRouteChangeContent());
        debugDetail.setFirewallPolicyChange(detail.getFirewallPolicyChange());
        debugDetail.setPermissionChangeContent(detail.getPermissionChangeContent());
        debugDetail.setNetworkParameterChangeRecord(detail.getNetworkParameterChangeRecord());
        debugDetail.setRollbackPlan(detail.getRollbackPlan());
        debugDetail.setTestResult(detail.getTestResult());
        debugDetail.setRemark(detail.getRemark());

        if (debugDetail.getId() == null) {
            repairDebugDetailMapper.insert(debugDetail);
        } else {
            repairDebugDetailMapper.updateById(debugDetail);
        }
    }

    @Override
    @Transactional
    public void saveOpticalDetail(Long workOrderId, CompleteDTO.OpticalDetailDTO detail) {
        RepairOpticalDetail opticalDetail = repairOpticalDetailMapper.selectByWorkOrderId(workOrderId);
        if (opticalDetail == null) {
            opticalDetail = new RepairOpticalDetail();
            opticalDetail.setWorkOrderId(workOrderId);
        }

        opticalDetail.setFaultOpticalPoint(detail.getFaultOpticalPoint());
        opticalDetail.setOpticalRouteName(detail.getOpticalRouteName());
        opticalDetail.setCableSection(detail.getCableSection());
        opticalDetail.setCableLength(detail.getCableLength());
        opticalDetail.setOpticalPowerBefore(detail.getOpticalPowerBefore());
        opticalDetail.setOpticalPowerAfter(detail.getOpticalPowerAfter());
        opticalDetail.setAttenuationBefore(detail.getAttenuationBefore());
        opticalDetail.setAttenuationAfter(detail.getAttenuationAfter());
        opticalDetail.setWavelength(detail.getWavelength());
        opticalDetail.setSplitterStatus(detail.getSplitterStatus());
        opticalDetail.setJumperStatus(detail.getJumperStatus());
        opticalDetail.setCableDamageDescription(detail.getCableDamageDescription());
        opticalDetail.setLinkTestResult(detail.getLinkTestResult());
        opticalDetail.setTestTool(detail.getTestTool());
        opticalDetail.setTestPerson(detail.getTestPerson());
        opticalDetail.setRemark(detail.getRemark());

        if (opticalDetail.getId() == null) {
            repairOpticalDetailMapper.insert(opticalDetail);
        } else {
            repairOpticalDetailMapper.updateById(opticalDetail);
        }
    }

    @Override
    public List<OrderLogVO> getOrderLogs(Long workOrderId) {
        return repairStatusLogMapper.selectByWorkOrderId(workOrderId).stream()
                .map(this::toOrderLogVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RepairWorkOrderVO> exportOrders(RepairOrderQueryDTO queryDTO) {
        // 导出必须复用列表页的全部筛选条件，否则「按优先级=HIGH 筛选后导出」
        // 导出的却是全部工单——数据与页面不一致。
        // 之前只手动拷了 4 个字段（workOrderNo/deviceCode/repairType/status），
        // 丢了 deviceName/repairUserId/reporterId/groupId/faultTimeStart/faultTimeEnd/priority。
        RepairOrderQueryDTO exportQuery = new RepairOrderQueryDTO();
        exportQuery.setPage(1);
        exportQuery.setPageSize(Integer.MAX_VALUE);
        exportQuery.setWorkOrderNo(queryDTO.getWorkOrderNo());
        exportQuery.setDeviceCode(queryDTO.getDeviceCode());
        exportQuery.setDeviceName(queryDTO.getDeviceName());
        exportQuery.setRepairType(queryDTO.getRepairType());
        exportQuery.setStatus(queryDTO.getStatus());
        exportQuery.setRepairUserId(queryDTO.getRepairUserId());
        exportQuery.setReporterId(queryDTO.getReporterId());
        exportQuery.setGroupId(queryDTO.getGroupId());
        exportQuery.setFaultTimeStart(queryDTO.getFaultTimeStart());
        exportQuery.setFaultTimeEnd(queryDTO.getFaultTimeEnd());
        exportQuery.setPriority(queryDTO.getPriority());

        // 返回 VO：报修人/维修人/创建时间等字段需解析后才能导出，实体字段不完整
        return convertToVOList(queryOrders(exportQuery).getRecords());
    }

    @Override
    public Page<RepairWorkOrderVO> listOrdersByDeviceId(Long deviceId, RepairOrderQueryDTO queryDTO) {
        Page<RepairWorkOrder> entityPage = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        LambdaQueryWrapper<RepairWorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairWorkOrder::getDeviceId, deviceId);
        wrapper.orderByDesc(RepairWorkOrder::getCreatedAt);
        Page<RepairWorkOrder> result = repairWorkOrderMapper.selectPage(entityPage, wrapper);

        Page<RepairWorkOrderVO> voPage =
                new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(convertToVOList(result.getRecords()));
        return voPage;
    }

    /** 批量转换，统一解析 reporter/repairUser/acceptor 姓名，避免 N+1 */
    private List<RepairWorkOrderVO> convertToVOList(List<RepairWorkOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = new java.util.HashSet<>();
        for (RepairWorkOrder order : orders) {
            if (order.getReporterId() != null) {
                userIds.add(order.getReporterId());
            }
            if (order.getRepairUserId() != null) {
                userIds.add(order.getRepairUserId());
            }
            if (order.getAcceptanceUserId() != null) {
                userIds.add(order.getAcceptanceUserId());
            }
        }

        Map<Long, String> userNames = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(),
                        (a, b) -> a));

        return orders.stream()
                .map(order -> convertToVO(order, userNames))
                .collect(Collectors.toList());
    }

    private OrderLogVO toOrderLogVO(RepairStatusLog log) {
        OrderLogVO vo = new OrderLogVO();
        vo.setId(log.getId());
        vo.setOrderNo(log.getWorkOrderNo());
        vo.setFromStatus(log.getOriginalStatus());
        vo.setToStatus(log.getNewStatus());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(StringUtils.hasText(log.getOperatorName())
                ? log.getOperatorName() : log.getCreatedBy());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getOperateTime() != null ? log.getOperateTime() : log.getCreatedAt());
        return vo;
    }

    private RepairWorkOrderVO convertToVO(RepairWorkOrder order, Map<Long, String> userNames) {
        RepairWorkOrderVO vo = new RepairWorkOrderVO();
        vo.setId(order.getId());
        vo.setWorkOrderNo(order.getWorkOrderNo());
        vo.setDeviceId(order.getDeviceId());
        vo.setDeviceCode(order.getDeviceCode());
        vo.setDeviceName(order.getDeviceName());
        vo.setGroupId(order.getGroupId());
        vo.setFaultTime(order.getFaultTime());
        vo.setReporterId(order.getReporterId());
        vo.setReporterName(order.getReporterId() == null ? null : userNames.get(order.getReporterId()));
        vo.setRepairType(order.getRepairType());
        vo.setRepairUserId(order.getRepairUserId());
        vo.setRepairUserName(order.getRepairUserId() == null ? null : userNames.get(order.getRepairUserId()));
        vo.setStatus(order.getStatus());
        vo.setCompletedTime(order.getCompletedTime());
        vo.setRepairDuration(order.getRepairDuration());
        vo.setFaultDescription(order.getFaultDescription());
        vo.setRepairSolution(order.getRepairSolution());
        vo.setRepairResult(order.getRepairResult());
        vo.setDeviceRepairStatus(order.getDeviceRepairStatus());
        vo.setAcceptorId(order.getAcceptanceUserId());
        vo.setAcceptorName(order.getAcceptanceUserId() == null ? null : userNames.get(order.getAcceptanceUserId()));
        vo.setAcceptTime(order.getAcceptanceTime());
        vo.setOverdue(order.getOverdue());
        vo.setPriority(order.getPriority());
        vo.setPlanCompleteTime(order.getPlanCompleteTime());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreatedAt());
        vo.setUpdateTime(order.getUpdatedAt());
        // rejectReason / repairStartTime / repairEndTime 无数据库列，
        // 由 getOrderVOById 从状态流转日志推导后回填
        return vo;
    }

    private void saveStatusLog(Long workOrderId, String workOrderNo, String originalStatus, String newStatus, Long operatorId, String remark) {
        RepairStatusLog log = new RepairStatusLog();
        log.setWorkOrderId(workOrderId);
        log.setWorkOrderNo(workOrderNo);
        log.setOriginalStatus(originalStatus);
        log.setNewStatus(newStatus);
        log.setOperatorId(operatorId);
        log.setOperateTime(LocalDateTime.now());
        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator != null) {
            log.setOperatorName(operator.getRealName());
        }
        log.setRemark(remark);
        repairStatusLogMapper.insert(log);
    }

    /**
     * 变更设备状态，并**同步写入设备状态变更日志**。
     *
     * <p>此前工单流转（分配/验收/返修）是直接 {@code networkDeviceMapper.updateById(device)} 改状态的，
     * 绕过了 {@code device_status_log}。而 {@code NetworkDeviceServiceImpl.updateStatus}
     * （手工改状态）是会写日志的——两条路径不一致。
     * 结果是设备详情页的「状态变更记录」只显示手工改动，
     * **恰好漏掉了工单驱动的那部分**（而在这个系统里，那才是设备状态变化的主要来源）。
     *
     * @param remark 日志备注，用于说明这次变更由哪个环节触发
     */
    private void changeDeviceStatus(NetworkDevice device, String newStatus, String remark) {
        if (device == null || Objects.equals(device.getStatus(), newStatus)) {
            return;
        }
        String originalStatus = device.getStatus();
        device.setStatus(newStatus);
        networkDeviceMapper.updateById(device);

        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(device.getId());
        log.setDeviceCode(device.getDeviceCode());
        log.setOriginalStatus(originalStatus);
        log.setNewStatus(newStatus);
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        deviceStatusLogMapper.insert(log);
    }

    /**
     * 发送站内通知，**失败不影响主流程，且只在业务事务提交成功后才发**。
     *
     * <p>为什么必须注册 {@code afterCommit} 回调，而不能直接调用：
     * <ol>
     *   <li>{@code NotificationServiceImpl.sendNotification} 自身带 {@code @Transactional}，
     *       在本方法（同样处于事务中）里直接调用会**加入同一个事务**。此时若写通知失败，
     *       Spring 会把当前事务标记为 rollback-only——**外面套 try/catch 也拦不住**，
     *       最终提交阶段会抛 {@code UnexpectedRollbackException}，把整个工单状态流转一起回滚。
     *       这属于「附属功能拖垮主流程」。</li>
     *   <li>反过来，若工单状态流转本身失败回滚，已发出的通知就成了「通知了一件没发生的事」。</li>
     * </ol>
     * 注册 {@code afterCommit} 回调可同时解决这两点：事务提交后才真正发通知，
     * 失败也不会影响已提交的业务数据。
     *
     * <p>另外两点约定：接收人为空（如工单尚未分配维修人）或接收人就是操作人时跳过——不给自己发通知；
     * 标题统一带工单号，便于在通知列表里定位。
     *
     * <p><b>relatedType / relatedId 必须按前端约定填</b>：前端
     * {@code layout/components/Navbar.vue#handleNotificationClick} 的跳转条件是
     * {@code relatedType === 'repair' && relatedId} 且 relatedId 为数字主键
     * （device 同理为 {@code 'device'}）。填成工单号或 null 都会导致「点通知不跳转」。
     */
    private void notifyAfterCommit(Long receiverId, Long operatorId, RepairWorkOrder order,
                                   String title, String content) {
        if (receiverId == null || receiverId.equals(operatorId)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSendNotification(receiverId, order.getId(), "repair", title, content);
                }
            });
        } else {
            doSendNotification(receiverId, order.getId(), "repair", title, content);
        }
    }

    private void doSendNotification(Long receiverId, Long relatedId, String relatedType, String title, String content) {
        try {
            notificationService.sendNotification(receiverId, title, content, "REPAIR_ORDER", relatedId, relatedType);
        } catch (Exception e) {
            logger.warn("发送工单通知失败：receiverId={}, relatedId={}", receiverId, relatedId, e);
        }
    }

    @Override
    public List<AttachmentVO> listAttachments(Long workOrderId) {
        List<RepairAttachment> attachments = repairAttachmentMapper.selectByWorkOrderId(workOrderId);
        if (attachments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> uploaderIds = attachments.stream()
                .map(RepairAttachment::getUploaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> uploaderNames = uploaderIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(uploaderIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(),
                        (a, b) -> a));

        return attachments.stream()
                .map(attachment -> toAttachmentVO(attachment,
                        attachment.getUploaderId() != null ? uploaderNames.get(attachment.getUploaderId()) : null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AttachmentVO uploadAttachment(Long workOrderId, MultipartFile file) {
        getOrderById(workOrderId);

        UploadResultVO stored = fileStorageService.store(file, "repair");

        RepairAttachment attachment = new RepairAttachment();
        attachment.setWorkOrderId(workOrderId);
        attachment.setFileName(stored.getFileName());
        attachment.setFilePath(stored.getUrl());
        attachment.setFileSize(stored.getFileSize());
        attachment.setFileType(stored.getFileType());
        attachment.setUploadTime(LocalDateTime.now());

        LoginUser loginUser = getCurrentLoginUser();
        attachment.setUploaderId(loginUser.getId());

        repairAttachmentMapper.insert(attachment);

        return toAttachmentVO(attachment, loginUser.getUsername());
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        RepairWorkOrder order = repairWorkOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST.getCode(), "工单不存在");
        }

        // 级联删除附件（含物理文件；物理文件删除失败不阻断记录清理）
        List<RepairAttachment> attachments = repairAttachmentMapper.selectList(
                new LambdaQueryWrapper<RepairAttachment>().eq(RepairAttachment::getWorkOrderId, id));
        for (RepairAttachment attachment : attachments) {
            repairAttachmentMapper.deleteById(attachment.getId());
            try {
                fileStorageService.delete(attachment.getFilePath());
            } catch (Exception e) {
                logger.warn("删除工单附件物理文件失败: {}", attachment.getFilePath(), e);
            }
        }

        // 级联删除状态日志与维修明细
        repairStatusLogMapper.delete(new LambdaQueryWrapper<RepairStatusLog>().eq(RepairStatusLog::getWorkOrderId, id));
        repairHardwareDetailMapper.delete(new LambdaQueryWrapper<RepairHardwareDetail>().eq(RepairHardwareDetail::getWorkOrderId, id));
        repairDebugDetailMapper.delete(new LambdaQueryWrapper<RepairDebugDetail>().eq(RepairDebugDetail::getWorkOrderId, id));
        repairOpticalDetailMapper.delete(new LambdaQueryWrapper<RepairOpticalDetail>().eq(RepairOpticalDetail::getWorkOrderId, id));

        // 删除工单本身
        repairWorkOrderMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteAttachment(Long workOrderId, Long attachmentId) {
        RepairAttachment attachment = repairAttachmentMapper.selectById(attachmentId);
        if (attachment == null || !Objects.equals(attachment.getWorkOrderId(), workOrderId)) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST.getCode(), "附件不存在");
        }

        repairAttachmentMapper.deleteById(attachmentId);
        fileStorageService.delete(attachment.getFilePath());
    }

    private AttachmentVO toAttachmentVO(RepairAttachment attachment, String uploaderName) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setWorkOrderId(attachment.getWorkOrderId());
        vo.setFileName(attachment.getFileName());
        vo.setFilePath(attachment.getFilePath());
        vo.setFileSize(attachment.getFileSize());
        vo.setFileType(attachment.getFileType());
        vo.setUploadTime(attachment.getUploadTime() != null
                ? attachment.getUploadTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);
        vo.setUploaderId(attachment.getUploaderId());
        vo.setUploaderName(uploaderName);
        return vo;
    }

    private String generateWorkOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer maxSeq = repairWorkOrderMapper.getMaxWorkOrderNoOfDay();
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
        return String.format("WO-%s-%06d", dateStr, nextSeq);
    }

    private LoginUser getCurrentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
