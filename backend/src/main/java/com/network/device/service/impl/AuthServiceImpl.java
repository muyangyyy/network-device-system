package com.network.device.service.impl;

import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.dto.ChangePasswordDTO;
import com.network.device.dto.LoginDTO;
import com.network.device.entity.LoginLog;
import com.network.device.entity.SysUser;
import com.network.device.entity.SysDepartment;
import com.network.device.mapper.LoginLogMapper;
import com.network.device.mapper.SysDepartmentMapper;
import com.network.device.security.JwtTokenProvider;
import com.network.device.security.LoginUser;
import com.network.device.security.UserDetailsImpl;
import com.network.device.service.AuthService;
import com.network.device.service.SysPermissionService;
import com.network.device.service.SysUserService;
import com.network.device.vo.LoginVO;
import com.network.device.vo.UserVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService sysUserService;
    private final SysPermissionService sysPermissionService;
    private final LoginLogMapper loginLogMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider,
                           SysUserService sysUserService,
                           SysPermissionService sysPermissionService,
                           LoginLogMapper loginLogMapper,
                           SysDepartmentMapper sysDepartmentMapper,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserService = sysUserService;
        this.sysPermissionService = sysPermissionService;
        this.loginLogMapper = loginLogMapper;
        this.sysDepartmentMapper = sysDepartmentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO, String ip) {
        // 注意：本方法**不能**加 @Transactional。
        // 登录失败分支需要先落一条失败日志再抛异常，若整个方法在一个事务里，
        // 抛出的 BusinessException 会连带把这条日志一起回滚掉，导致「登录日志」里永远没有失败记录。
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(loginDTO.getUsername());
        loginLog.setLoginIp(ip);
        loginLog.setLoginTime(LocalDateTime.now());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // principal 是 UserDetailsServiceImpl 返回的 UserDetailsImpl（含 id/roles），
            // 不是 LoginUser——后者只在 JWT 过滤器解析 token 后构造。此前强转 LoginUser
            // 会 ClassCastException，被兜底 catch 吞成「用户名或密码错误」。
            UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
            SysUser user = sysUserService.getUserById(loginUser.getId());

            if (user.getStatus() != null && user.getStatus() == 0) {
                loginLog.setStatus(0);
                loginLog.setMsg("账号已被禁用");
                loginLogMapper.insert(loginLog);
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号已被禁用");
            }

            List<String> roles = loginUser.getRoles();
            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roles);

            List<String> permissions = sysPermissionService.getPermKeysByUserId(user.getId());

            // 最后登录时间/IP 必须用专用方法落库：
            // updateUser 走的是「按 UserDTO 非空字段覆盖」，而 UserDTO 里没有这两个字段，用它写等于没写。
            LocalDateTime loginTime = LocalDateTime.now();
            user.setLastLoginTime(loginTime);
            user.setLastLoginIp(ip);
            sysUserService.recordLogin(user.getId(), loginTime, ip);

            loginLog.setStatus(1);
            loginLog.setMsg("登录成功");
            loginLogMapper.insert(loginLog);

            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setUserId(user.getId());
            loginVO.setUsername(user.getUsername());
            loginVO.setRealName(user.getRealName());
            loginVO.setNickname(user.getRealName() != null && !user.getRealName().isEmpty()
                    ? user.getRealName() : user.getUsername());
            loginVO.setRoles(roles);
            loginVO.setPermissions(permissions);

            return loginVO;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            loginLog.setStatus(0);
            loginLog.setMsg(e.getMessage());
            loginLogMapper.insert(loginLog);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public UserVO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = sysUserService.getUserById(loginUser.getId());

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setNickname(user.getRealName() != null && !user.getRealName().isEmpty()
                ? user.getRealName() : user.getUsername());
        userVO.setPhone(user.getPhone());
        userVO.setEmail(user.getEmail());
        userVO.setAvatar(user.getAvatar());
        userVO.setDepartmentId(user.getDepartmentId());
        // 个人信息页展示部门名：不填则前端永远显示「-」（已删除部门时为 null，显示「-」是合理兜底）
        if (user.getDepartmentId() != null) {
            SysDepartment dept = sysDepartmentMapper.selectById(user.getDepartmentId());
            userVO.setDepartmentName(dept != null ? dept.getDeptName() : null);
        }
        userVO.setStatus(user.getStatus());
        userVO.setLastLoginTime(user.getLastLoginTime());
        userVO.setRoles(loginUser.getRoles());
        userVO.setPermissions(sysPermissionService.getPermKeysByUserId(user.getId()));

        return userVO;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "两次输入的密码不一致");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = sysUserService.getUserById(loginUser.getId());

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "旧密码错误");
        }

        // 必须用 updatePassword：原先调用 updateUser 时，UserDTO 不携带 password，
        // 而 updateUser 是「重新查库再覆盖」，结果新密码根本没写进库
        // ——接口返回成功，旧密码依然能登录。
        sysUserService.updatePassword(user.getId(), passwordEncoder.encode(dto.getNewPassword()));
    }
}
