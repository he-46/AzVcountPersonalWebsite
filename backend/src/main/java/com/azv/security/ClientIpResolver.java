package com.azv.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    private static final Pattern NUMERIC_IP = Pattern.compile("[0-9a-fA-F:.]+");

    private final boolean trustForwardedHeaders;

    public ClientIpResolver(
            @Value("${security.client-ip.trust-forwarded-headers:false}")
            boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public String resolve(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null) {
                String[] candidates = forwardedFor.split(",");
                // 从离应用最近的一跳开始，避免单层代理使用 append 模式时信任客户端伪造的首项。
                for (int i = candidates.length - 1; i >= 0; i--) {
                    String normalized = normalize(candidates[i].trim());
                    if (normalized != null) {
                        return normalized;
                    }
                }
            }
        }

        String remoteAddress = normalize(request.getRemoteAddr());
        return remoteAddress == null ? "unknown" : remoteAddress;
    }

    private String normalize(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > 45
                || !NUMERIC_IP.matcher(candidate).matches()) {
            return null;
        }
        try {
            return InetAddress.getByName(candidate).getHostAddress();
        } catch (UnknownHostException ignored) {
            return null;
        }
    }
}
