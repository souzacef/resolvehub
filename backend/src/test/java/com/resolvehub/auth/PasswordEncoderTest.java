package com.resolvehub.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PasswordEncoderTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordIsHashedWithBcryptAndMatchesRawValue() {
        String rawPassword = "StrongPass123!";
        String hashed = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, hashed);
        assertTrue(passwordEncoder.matches(rawPassword, hashed));
    }
}
