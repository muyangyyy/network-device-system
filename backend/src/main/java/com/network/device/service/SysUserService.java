package com.network.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.dto.UserDTO;
import com.network.device.dto.UserQueryDTO;
import com.network.device.entity.SysUser;
import com.network.device.vo.UserVO;

import java.time.LocalDateTime;

public interface SysUserService {

    Page<SysUser> listUsers(UserQueryDTO queryDTO);

    /** 分页查询用户，返回已补齐部门名/角色信息、且字段名对齐前端的 VO */
    Page<UserVO> listUserVOs(UserQueryDTO queryDTO);

    SysUser getUserById(Long id);

    /** 单个用户 VO（含部门名与角色信息） */
    UserVO getUserVOById(Long id);

    /** 实体转 VO（不做关联查询，仅字段映射） */
    UserVO toVO(SysUser user);

    SysUser createUser(UserDTO userDTO);

    SysUser updateUser(Long id, UserDTO userDTO);

    void deleteUser(Long id);

    /**
     * 重置用户密码为随机值并返回明文。
     *
     * <p>此前硬编码为 {@code "123456"}——每次重置都是同一个密码，
     * 且不强制首次登录修改。改为生成 8 位随机密码（大小写字母 + 数字），
     * 返回给操作者一次性展示，由操作者转交用户。
     */
    String resetPassword(Long id);

    /**
     * 仅更新密码（入参需为已加密的密文）。
     *
     * <p>必须独立成方法：{@link #updateUser(Long, UserDTO)} 是「重新查库 + 按 DTO 非空字段覆盖」的语义，
     * 而 UserDTO 不携带 password，用它改密码会静默失效。
     */
    void updatePassword(Long id, String encodedPassword);

    /**
     * 记录登录成功信息（最后登录时间 / IP）。
     *
     * <p>同样不能走 {@link #updateUser(Long, UserDTO)}——UserDTO 没有这两个字段。
     */
    void recordLogin(Long id, LocalDateTime loginTime, String loginIp);

    void updateStatus(Long id, Integer status);

    SysUser getUserByUsername(String username);
}
