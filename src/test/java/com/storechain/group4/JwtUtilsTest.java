package com.storechain.group4;

import com.storechain.group4.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L);
    }

    @Test
    void testGenerateAndValidateJwtToken() {
        String username = "testuser";
        String token = jwtUtils.generateTokenFromUsername(username);

        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals(username, jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void testInvalidJwtToken() {
        assertFalse(jwtUtils.validateJwtToken("invalid.token.structure"));
    }
}

