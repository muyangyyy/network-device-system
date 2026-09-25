package com.network.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.vo.LoginLogVO;

public interface LoginLogService {

    Page<LoginLogVO> listLoginLogs(String username, Integer page, Integer pageSize);
}
