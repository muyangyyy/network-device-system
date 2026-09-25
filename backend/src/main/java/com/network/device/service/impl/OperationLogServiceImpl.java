package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.dto.LogQueryDTO;
import com.network.device.entity.OperationLog;
import com.network.device.mapper.OperationLogMapper;
import com.network.device.service.OperationLogService;
import com.network.device.vo.OperationLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void saveLog(OperationLog log) {
        operationLogMapper.insert(log);
    }

    @Override
    public Page<OperationLog> listLogs(String module, Integer page, Integer pageSize) {
        Page<OperationLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }

        wrapper.orderByDesc(OperationLog::getOperateTime);
        return operationLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<OperationLogVO> listLogVO(LogQueryDTO query) {
        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getOperatorName())) {
            wrapper.like(OperationLog::getOperatorName, query.getOperatorName());
        }
        if (StringUtils.hasText(query.getModule())) {
            wrapper.like(OperationLog::getModule, query.getModule());
        }
        applyOperationType(wrapper, query.getOperationType());
        applyTimeRange(wrapper, query.getStartTime(), query.getEndTime());

        wrapper.orderByDesc(OperationLog::getOperateTime);

        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<OperationLogVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<OperationLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    /** 操作类型过滤：依据 @OperationLog 注解里的 operation 文案做模糊匹配 */
    private void applyOperationType(LambdaQueryWrapper<OperationLog> wrapper, String type) {
        if (!StringUtils.hasText(type)) {
            return;
        }
        String upper = type.trim().toUpperCase();
        if ("LOGIN".equals(upper)) {
            wrapper.like(OperationLog::getOperation, "登录");
        } else if ("LOGOUT".equals(upper)) {
            wrapper.and(w -> w.like(OperationLog::getOperation, "登出")
                    .or().like(OperationLog::getOperation, "退出"));
        } else if ("CREATE".equals(upper)) {
            wrapper.and(w -> w.like(OperationLog::getOperation, "创建")
                    .or().like(OperationLog::getOperation, "新增")
                    .or().like(OperationLog::getOperation, "导入"));
        } else if ("UPDATE".equals(upper)) {
            wrapper.and(w -> w.like(OperationLog::getOperation, "更新")
                    .or().like(OperationLog::getOperation, "修改")
                    .or().like(OperationLog::getOperation, "分配")
                    .or().like(OperationLog::getOperation, "验收")
                    .or().like(OperationLog::getOperation, "提交")
                    .or().like(OperationLog::getOperation, "延期")
                    .or().like(OperationLog::getOperation, "返修"));
        } else if ("DELETE".equals(upper)) {
            wrapper.like(OperationLog::getOperation, "删除");
        }
    }

    private void applyTimeRange(LambdaQueryWrapper<OperationLog> wrapper, String startTime, String endTime) {
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(OperationLog::getOperateTime, LocalDate.parse(startTime).atStartOfDay());
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(OperationLog::getOperateTime, LocalDate.parse(endTime).atTime(23, 59, 59));
        }
    }

    private OperationLogVO toVO(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setOperatorName(log.getOperatorName());
        vo.setOperationType(resolveOperationType(log));
        vo.setModule(log.getModule());
        vo.setDescription(log.getOperation());
        vo.setIp(log.getOperatorIp());
        vo.setStatus(log.getStatus());
        vo.setCreateTime(log.getOperateTime() != null ? log.getOperateTime().format(TIME_FORMAT) : null);
        return vo;
    }

    /** 从操作文案推导前端所需的标准操作类型 */
    private String resolveOperationType(OperationLog log) {
        String operation = log.getOperation();
        if (StringUtils.hasText(operation)) {
            if (operation.contains("登录")) return "LOGIN";
            if (operation.contains("登出") || operation.contains("退出")) return "LOGOUT";
            if (operation.contains("创建") || operation.contains("新增") || operation.contains("导入")) return "CREATE";
            if (operation.contains("更新") || operation.contains("修改") || operation.contains("分配")
                    || operation.contains("验收") || operation.contains("提交") || operation.contains("延期")
                    || operation.contains("返修")) return "UPDATE";
            if (operation.contains("删除")) return "DELETE";
        }
        String method = log.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) return "CREATE";
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) return "UPDATE";
        if ("DELETE".equalsIgnoreCase(method)) return "DELETE";
        return "OTHER";
    }
}
