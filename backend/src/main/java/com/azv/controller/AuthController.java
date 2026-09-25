package com.azv.controller;

import com.azv.common.BizException;
import com.azv.common.R;
import com.azv.entity.User;
import com.azv.mapper.UserMapper;
import com.azv.security.ClientIpResolver;
import com.azv.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final ClientIpResolver clientIpResolver;

    @Value("${security.auth-cookie.secure:false}")
    private boolean secureCookie;

    private static final String COOKIE_NAME = "azv_token";
    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_SECONDS = Duration.ofSeconds(900);

    @PostMapping("/login")
    public R<Void> login(@RequestBody LoginRequest loginRequest,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String username = loginRequest.username() == null ? "" : loginRequest.username().trim();
        String password = loginRequest.password() == null ? "" : loginRequest.password();
        if (username.isEmpty() || username.length() > 50
                || password.isEmpty() || password.length() > 200) {
            throw new BizException("用户名或密码错误");
        }

        // IP 只从服务端连接信息或显式信任的反向代理头读取，不接受请求体传值。
        String ip = clientIpResolver.resolve(request);
        String ipLockKey = "login:fail:ip:" + ip;
        String accountLockKey = "login:fail:account:" + username.toLowerCase(Locale.ROOT);
        if (isLocked(redis.opsForValue().get(ipLockKey))
                || isLocked(redis.opsForValue().get(accountLockKey))) {
            throw new BizException("失败次数过多，请15分钟后再试");
        }

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        if (user == null || !Objects.equals(user.getStatus(), 1)
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            recordFailure(ipLockKey);
            recordFailure(accountLockKey);
            throw new BizException("用户名或密码错误");
        }

        redis.delete(List.of(ipLockKey, accountLockKey));
        String token = jwtUtil.generate(user.getId(), user.getRole());
        response.addCookie(authCookie(token, cookieMaxAge()));
        return R.ok(null);
    }

    @PostMapping("/logout")
    public R<Void> logout(@CookieValue(value = COOKIE_NAME, required = false) String token,
                          HttpServletResponse response) {
        try {
            revokeIfValid(token);
        } finally {
            response.addCookie(authCookie("", 0));
        }
        return R.ok(null);
    }

    private void revokeIfValid(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException | IllegalArgumentException ignored) {
            return;
        }

        String jti = claims.getId();
        if (jti == null || jti.isBlank() || claims.getExpiration() == null) {
            return;
        }
        Duration remaining = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        if (!remaining.isNegative() && !remaining.isZero()) {
            redis.opsForValue().set("jwt:revoked:" + jti, "1", remaining);
        }
    }

    private boolean isLocked(String failCount) {
        if (failCount == null) {
            return false;
        }
        try {
            return Long.parseLong(failCount) >= MAX_FAIL;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private void recordFailure(String key) {
        Long attempts = redis.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redis.expire(key, LOCK_SECONDS);
        }
    }

    private int cookieMaxAge() {
        return (int) Math.min(Integer.MAX_VALUE, jwtUtil.expirationDuration().toSeconds());
    }

    private Cookie authCookie(String value, int maxAge) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    public record LoginRequest(String username, String password) {
    }
}
