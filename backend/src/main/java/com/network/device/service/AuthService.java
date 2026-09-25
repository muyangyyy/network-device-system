package com.network.device.service;

import com.network.device.dto.ChangePasswordDTO;
import com.network.device.dto.LoginDTO;
import com.network.device.vo.LoginVO;
import com.network.device.vo.UserVO;

public interface AuthService {

    LoginVO login(LoginDTO loginDTO, String ip);

    void logout();

    UserVO getCurrentUser();

    void changePassword(ChangePasswordDTO dto);
}
