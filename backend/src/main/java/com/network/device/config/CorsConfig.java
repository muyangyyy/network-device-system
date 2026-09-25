package com.network.device.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    /**
     * 生产部署源，多个用逗号分隔，如 http://192.168.1.10:8088,https://demo.example.com。
     * 经环境变量 CORS_ALLOWED_ORIGINS 注入（Spring relaxed binding）。
     * 前端经 nginx 反代 /api 同源访问时无需配置。
     */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 开发源（Vite 本地端口）
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");
        // 生产环境部署源：同源页面发起的 POST 浏览器仍会携带 Origin 头（Fetch 规范），
        // 白名单缺了部署源，CorsFilter 会把登录等全部写请求判为非法源直接 403——
        // 且 curl 不带 Origin 测不出此问题，只有真实浏览器会踩中。
        if (StringUtils.hasText(allowedOrigins)) {
            for (String origin : allowedOrigins.split(",")) {
                if (StringUtils.hasText(origin)) {
                    config.addAllowedOriginPattern(origin.trim());
                }
            }
        }
        // 内网网段直连（家庭/办公局域网）
        config.addAllowedOriginPattern("http://192.168.*:*");
        config.addAllowedOriginPattern("http://10.*:*");
        config.addAllowedOriginPattern("http://172.*:*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
