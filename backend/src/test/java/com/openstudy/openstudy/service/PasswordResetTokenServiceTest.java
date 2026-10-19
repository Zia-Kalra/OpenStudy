package com.openstudy.openstudy.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.openstudy.openstudy.model.User;
import org.junit.jupiter.api.Test;

class PasswordResetTokenServiceTest {

    private final PasswordResetTokenService service =
            new PasswordResetTokenService("unit-test-reset-secret-at-least-32-chars");

    @Test
    void resetTokenIsSignedAndBoundToTheCurrentPasswordHash() {
        User user = new User(42L, "Student", "student@example.com", "$2a$10$old-password-hash", "student", "ACTIVE");

        String token = service.createToken(user);
        var claims = service.parse(token);

        assertThat(claims).isNotNull();
        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(service.matchesPassword(claims, user.getPasswordHash())).isTrue();
        assertThat(service.matchesPassword(claims, "$2a$10$new-password-hash")).isFalse();
        assertThat(service.parse(token + "tampered")).isNull();
    }
}