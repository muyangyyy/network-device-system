package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.dto.UserDTO;
import com.network.device.dto.UserQueryDTO;
import com.network.device.entity.SysDepartment;
import com.network.device.entity.SysRole;
import com.network.device.entity.SysUser;
import com.network.device.entity.SysUserRole;
import com.network.device.mapper.SysDepartmentMapper;
import com.network.device.mapper.SysRoleMapper;
import com.network.device.mapper.SysUserMapper;
import com.network.device.mapper.SysUserRoleMapper;
import com.network.device.service.SysUserService;
import com.network.device.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserMapper sysUserMapper,
                              SysUserRoleMapper sysUserRoleMapper,
                              SysRoleMapper sysRoleMapper,
                              SysDepartmentMapper sysDepartmentMapper,
                              PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysDepartmentMapper = sysDepartmentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Page<SysUser> listUsers(UserQueryDTO queryDTO) {
        Page<SysUser> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        return sysUserMapper.selectPage(page, buildWrapper(queryDTO));
    }

    @Override
    public Page<UserVO> listUserVOs(UserQueryDTO queryDTO) {
        Page<SysUser> page = listUsers(queryDTO);
        List<SysUser> records = page.getRecords();

        // 批量回填部门名与角色，避免 N+1
        Map<Long, String> deptNames = loadDepartmentNames(records);
        Map<Long, List<Long>> roleIdMap = loadRoleIds(
                records.stream().map(SysUser::getId).collect(Collectors.toList()));
        Map<Long, String> roleNameMap = loadRoleNames(roleIdMap.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet()));

        List<UserVO> vos = records.stream()
                .map(user -> toVO(user,
                        deptNames.get(user.getDepartmentId()),
                        roleIdMap.getOrDefault(user.getId(), Collections.emptyList()),
                        roleNameMap))
                .collect(Collectors.toList());

        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public SysUser getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return user;
    }

    @Override
    public UserVO getUserVOById(Long id) {
        SysUser user = getUserById(id);
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(id);
        Map<Long, String> roleNameMap = loadRoleNames(roleIds.stream().collect(Collectors.toSet()));
        String deptName = user.getDepartmentId() == null ? null
                : loadDepartmentNames(Collections.singletonList(user)).get(user.getDepartmentId());
        return toVO(user, deptName, roleIds, roleNameMap);
    }

    @Override
    public UserVO toVO(SysUser user) {
        return toVO(user, null, Collections.emptyList(), Collections.emptyMap());
    }

    @Override
    @Transactional
    public SysUser createUser(UserDTO userDTO) {
        // 唯一性预检必须覆盖「已逻辑删除」的行：uk_username 是普通唯一索引，不区分逻辑删除，
        // 被删除的用户在物理上仍占用该用户名。
        // 这里不能用 selectCount(new LambdaQueryWrapper<>()...)——MyBatis-Plus 会自动追加
        // deleted = 0，导致删除用户后再建同名用户时预检通过，直到 INSERT 才撞唯一键，
        // 前端只能看到笼统的「数据已存在」，无法得知真实原因。
        Long exists = sysUserMapper.countByUsernameIncludeDeleted(userDTO.getUsername());
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DATA_EXIST.getCode(),
                    "用户名已存在（可能属于已删除的用户，请更换用户名）");
        }

        SysUser user = new SysUser();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRealName(userDTO.getRealName());
        user.setPhone(userDTO.getPhone());
        user.setEmail(userDTO.getEmail());
        user.setDepartmentId(userDTO.getDepartmentId());
        user.setStatus(userDTO.getStatus() != null ? userDTO.getStatus() : 1);
        sysUserMapper.insert(user);

        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            for (Long roleId : userDTO.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }

        return user;
    }

    @Override
    @Transactional
    public SysUser updateUser(Long id, UserDTO userDTO) {
        SysUser user = getUserById(id);

        if (StringUtils.hasText(userDTO.getRealName())) {
            user.setRealName(userDTO.getRealName());
        }
        if (StringUtils.hasText(userDTO.getPhone())) {
            user.setPhone(userDTO.getPhone());
        }
        if (StringUtils.hasText(userDTO.getEmail())) {
            user.setEmail(userDTO.getEmail());
        }
        // 0 是前端「清空」哨兵：el-tree-select 清空后值是 undefined，JSON 序列化会省略字段，
        // 后端无法区分「没传」与「清空」；部门 id 自增从 1 开始，0 不是合法 id。
        // 置 NULL 必须走 UpdateWrapper——updateById 的 NOT_NULL 策略会跳过 null 字段。
        boolean clearDepartment = userDTO.getDepartmentId() != null && userDTO.getDepartmentId() == 0L;
        if (userDTO.getDepartmentId() != null && !clearDepartment) {
            user.setDepartmentId(userDTO.getDepartmentId());
        }
        if (userDTO.getStatus() != null) {
            user.setStatus(userDTO.getStatus());
        }
        sysUserMapper.updateById(user);
        if (clearDepartment) {
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, id)
                    .set(SysUser::getDepartmentId, null));
        }

        if (userDTO.getRoleIds() != null) {
            sysUserRoleMapper.deleteByUserId(id);
            for (Long roleId : userDTO.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }

        return user;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        getUserById(id);
        sysUserMapper.deleteById(id);
    }

    private static final String RESET_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int RESET_PASSWORD_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public String resetPassword(Long id) {
        SysUser user = getUserById(id);
        // 生成随机密码：排除易混淆字符（0/O/1/l/I），8 位长度
        StringBuilder sb = new StringBuilder(RESET_PASSWORD_LENGTH);
        for (int i = 0; i < RESET_PASSWORD_LENGTH; i++) {
            sb.append(RESET_PASSWORD_CHARS.charAt(
                    SECURE_RANDOM.nextInt(RESET_PASSWORD_CHARS.length())));
        }
        String rawPassword = sb.toString();
        user.setPassword(passwordEncoder.encode(rawPassword));
        sysUserMapper.updateById(user);
        return rawPassword;
    }

    @Override
    @Transactional
    public void updatePassword(Long id, String encodedPassword) {
        SysUser user = getUserById(id);
        user.setPassword(encodedPassword);
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void recordLogin(Long id, LocalDateTime loginTime, String loginIp) {
        SysUser user = getUserById(id);
        user.setLastLoginTime(loginTime);
        user.setLastLoginIp(loginIp);
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysUser user = getUserById(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    @Override
    public SysUser getUserByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }

    // ------------------------------------------------------------------ helpers

    private LambdaQueryWrapper<SysUser> buildWrapper(UserQueryDTO queryDTO) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getUsername())) {
            wrapper.like(SysUser::getUsername, queryDTO.getUsername());
        }
        if (StringUtils.hasText(queryDTO.getRealName())) {
            wrapper.like(SysUser::getRealName, queryDTO.getRealName());
        }
        if (StringUtils.hasText(queryDTO.getPhone())) {
            wrapper.like(SysUser::getPhone, queryDTO.getPhone());
        }
        if (queryDTO.getDepartmentId() != null) {
            wrapper.eq(SysUser::getDepartmentId, queryDTO.getDepartmentId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, queryDTO.getStatus());
        }

        wrapper.orderByDesc(SysUser::getCreatedAt);
        return wrapper;
    }

    private Map<Long, String> loadDepartmentNames(List<SysUser> users) {
        Set<Long> deptIds = users.stream()
                .map(SysUser::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysDepartmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getDeptName, (a, b) -> a));
    }

    private Map<Long, List<Long>> loadRoleIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = sysUserRoleMapper.selectRoleIdsByUserIds(userIds);
        Map<Long, List<Long>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long userId = toLong(row.get("userId"));
            Long roleId = toLong(row.get("roleId"));
            if (userId == null || roleId == null) {
                continue;
            }
            map.computeIfAbsent(userId, k -> new ArrayList<>()).add(roleId);
        }
        return map;
    }

    private Map<Long, String> loadRoleNames(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    private UserVO toVO(SysUser user, String departmentName, List<Long> roleIds,
                        Map<Long, String> roleNameMap) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setNickname(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setDepartmentName(departmentName);
        vo.setRoleIds(roleIds == null ? Collections.emptyList() : roleIds);
        vo.setRoleNames(roleIds == null ? Collections.emptyList()
                : roleIds.stream().map(roleNameMap::get).filter(Objects::nonNull).collect(Collectors.toList()));
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreatedAt());
        vo.setUpdateTime(user.getUpdatedAt());
        return vo;
    }
}
