package com.network.device.security;

import com.network.device.service.SysPermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器。
 *
 * <p>除了写入 {@code ROLE_xxx} 角色外，还会把当前用户**实际的权限键**
 * （{@code device:delete} 这类，来自 sys_permission）一并注入 authorities，
 * 供 {@link PermissionService} 与 {@code @PreAuthorize("@ss.hasPermi(...)")} 判定。
 *
 * <p>权限键**每次请求实时查库**，因此管理员在「角色管理」里调整授权后立即生效，
 * 无需用户重新登录。代价是每个已认证请求多一次三表关联查询——本系统表数据量极小，
 * 开销可忽略；若后续用户量增大，可在此处加带 TTL 的内存缓存。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SysPermissionService sysPermissionService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   SysPermissionService sysPermissionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String username = jwtTokenProvider.getUsernameFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                }
                // 注入权限键，供 @PreAuthorize("@ss.hasPermi('xxx')") 判定
                authorities.addAll(loadPermissionAuthorities(userId));

                LoginUser loginUser = new LoginUser(userId, username, roles);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 认证失败不阻断请求，交由后续授权规则决定放行还是 401/403
            log.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 加载用户权限键并包装为 authority。
     *
     * <p>查库异常时**返回空集合而非抛出**——宁可让该用户被判为「无权限」（后续 403），
     * 也不要因为一次数据库抖动导致整个请求 500。
     */
    private List<SimpleGrantedAuthority> loadPermissionAuthorities(Long userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            List<String> permKeys = sysPermissionService.getPermKeysByUserId(userId);
            if (permKeys == null || permKeys.isEmpty()) {
                return List.of();
            }
            List<SimpleGrantedAuthority> list = new ArrayList<>(permKeys.size());
            for (String key : permKeys) {
                if (StringUtils.hasText(key)) {
                    list.add(new SimpleGrantedAuthority(key));
                }
            }
            return list;
        } catch (Exception e) {
            log.error("加载用户权限失败, userId={}", userId, e);
            return List.of();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
