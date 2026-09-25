package com.azv.security;

import com.azv.common.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminInterceptor(JwtUtil jwtUtil, StringRedisTemplate redis) {
        this.jwtUtil = jwtUtil;
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // ① 取 Cookie
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("azv_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        // ② 解析校验
        try {
            Claims claims = jwtUtil.parse(token);   // token 为 null 时 parse 抛异常
            if (!"ADMIN".equals(claims.get("role"))) {
                return deny(response, 403, "无权限");
            }
            String jti = claims.getId();
            if (jti == null || jti.isBlank()
                    || Boolean.TRUE.equals(redis.hasKey("jwt:revoked:" + jti))) {
                return deny(response, 401, "登录已失效，请重新登录");
            }
            request.setAttribute("userId", claims.getSubject());
            return true;   // 放行
        } catch (Exception e) {
            return deny(response, 401, "未登录或登录已过期");
        }
    }

    private boolean deny(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.error(message)));
        return false;
    }
}
