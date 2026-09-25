package com.azv.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void ignoresForwardedForByDefault() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "192.0.2.10");

        assertEquals("203.0.113.9", resolver.resolve(request));
    }

    @Test
    void trustedForwardingUsesNearestValidNumericAddress() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "198.51.100.23, forged.example, 192.0.2.10");

        assertEquals("192.0.2.10", resolver.resolve(request));
    }

    @Test
    void trustedForwardingRejectsNonNumericValuesAndFallsBackToRemoteAddress() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "attacker.example, 192.0.2.10.evil");

        assertEquals("203.0.113.9", resolver.resolve(request));
    }
}
