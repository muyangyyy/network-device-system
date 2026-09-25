package com.network.device.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 开发源（Vite 本地端口）
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");
        // 生产源：飞牛 NAS。
        // 关键：同源页面发起的 POST 浏览器仍会携带 Origin 头（Fetch 规范），
        // 白名单缺了它，CorsFilter 会把登录等全部写请求判为非法源直接 403——
        // 且 curl 不带 Origin 测不出此问题，只有真实浏览器会踩中。
        config.addAllowedOriginPattern("http://117.159.223.35:*");
        // 内网直连 NAS（家庭/办公局域网经内网 IP 访问）
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
