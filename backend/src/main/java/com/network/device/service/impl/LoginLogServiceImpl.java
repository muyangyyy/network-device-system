package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.entity.LoginLog;
import com.network.device.mapper.LoginLogMapper;
import com.network.device.service.LoginLogService;
import com.network.device.vo.LoginLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoginLogMapper loginLogMapper;

    public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public Page<LoginLogVO> listLoginLogs(String username, Integer page, Integer pageSize) {
        Page<LoginLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(LoginLog::getUsername, username);
        }
        wrapper.orderByDesc(LoginLog::getLoginTime);

        Page<LoginLog> result = loginLogMapper.selectPage(pageParam, wrapper);

        List<LoginLogVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<LoginLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private LoginLogVO toVO(LoginLog log) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(log.getId());
        vo.setUsername(log.getUsername());
        vo.setIp(log.getLoginIp());
        vo.setLoginLocation(log.getLoginLocation());
        vo.setBrowser(log.getBrowser());
        vo.setOs(log.getOs());
        vo.setSuccess(log.getStatus() != null && log.getStatus() == 1);
        vo.setMessage(log.getMsg());
        vo.setCreateTime(log.getLoginTime() != null ? log.getLoginTime().format(TIME_FORMAT) : null);
        return vo;
    }
}
