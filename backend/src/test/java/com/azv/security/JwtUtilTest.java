package com.azv.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void rejectsSecretsShorterThanHs256Minimum() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("too-short", 24));
    }

    @Test
    void issuesUniqueRevocableTokenIdentifiers() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 24);

        Claims first = jwtUtil.parse(jwtUtil.generate(42L, "ADMIN"));
        Claims second = jwtUtil.parse(jwtUtil.generate(42L, "ADMIN"));

        assertEquals("42", first.getSubject());
        assertEquals("ADMIN", first.get("role"));
        assertNotNull(first.getExpiration());
        assertNotNull(first.getId());
        assertNotEquals(first.getId(), second.getId());
    }
}
