package com.network.device.controller;

import com.network.device.common.Result;
import com.network.device.dto.ChangePasswordDTO;
import com.network.device.dto.LoginDTO;
import com.network.device.service.AuthService;
import com.network.device.vo.LoginVO;
import com.network.device.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "登录、登出、获取当前用户等接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        LoginVO loginVO = authService.login(loginDTO, ip);
        return Result.success("登录成功", loginVO);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("登出成功");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current-user")
    public Result<UserVO> getCurrentUser() {
        UserVO userVO = authService.getCurrentUser();
        return Result.success(userVO);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success("密码修改成功");
    }
}
