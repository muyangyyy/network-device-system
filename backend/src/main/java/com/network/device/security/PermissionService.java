package com.network.device.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接口级鉴权判定器，供 {@code @PreAuthorize("@ss.xxx(...)")} 使用。
 *
 * <p>判定依据是 {@link org.springframework.security.core.Authentication} 里的权限键
 * （由 {@link JwtAuthenticationFilter} 从数据库加载并注入）。
 * <b>不按角色放行</b>——即使是 ADMIN 角色，也必须真正拥有对应权限键，
 * 从而保证「前端按钮显隐」与「后端接口鉴权」使用完全同一套权限数据，
 * 不出现「界面看不到、但接口能调」的缺口。
 *
 * <p>约定：权限键为 {@code 模块:动作} 形式（如 {@code device:delete}）；
 * 特殊键 {@code *} 视为拥有全部权限（与前端 {@code hasPermission} 的兜底逻辑一致）。
 */
@Component("ss")
public class PermissionService {

    /** 通配符权限：拥有它即视为拥有全部权限 */
    private static final String ALL_PERMISSION = "*";

    /**
     * 是否拥有指定权限键。
     *
     * @param permission 权限键，如 {@code device:delete}
     */
    public boolean hasPermi(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        Set<String> owned = currentAuthorities();
        return owned.contains(ALL_PERMISSION) || owned.contains(permission);
    }

    /**
     * 是否拥有其中任意一个权限键。用于被多个模块共用的接口，
     * 例如「用户下拉列表」被设备/工单/统计等多个页面调用。
     *
     * <pre>{@code @PreAuthorize("@ss.hasAnyPermi('device:list','repair:add')")}</pre>
     */
    public boolean hasAnyPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        Set<String> owned = currentAuthorities();
        if (owned.contains(ALL_PERMISSION)) {
            return true;
        }
        return Arrays.stream(permissions)
                .filter(StringUtils::hasText)
                .anyMatch(owned::contains);
    }

    /** 是否拥有全部指定权限键 */
    public boolean hasAllPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        Set<String> owned = currentAuthorities();
        if (owned.contains(ALL_PERMISSION)) {
            return true;
        }
        return Arrays.stream(permissions)
                .filter(StringUtils::hasText)
                .allMatch(owned::contains);
    }

    /** 是否拥有指定角色（角色以 {@code ROLE_} 前缀存放在 authorities 中） */
    public boolean hasRole(String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }
        return currentAuthorities().contains("ROLE_" + role);
    }

    /** 当前登录用户持有的全部 authority（含权限键与 ROLE_ 角色） */
    private Set<String> currentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return Set.of();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    /** 便于调试：当前用户持有的权限键（不含 ROLE_ 前缀项） */
    public List<String> currentPermissionKeys() {
        return currentAuthorities().stream()
                .filter(a -> !a.startsWith("ROLE_"))
                .sorted()
                .collect(Collectors.toList());
    }
}
