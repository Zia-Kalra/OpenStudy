package com.openstudy.openstudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mail.javamail.JavaMailSender;

class DatabaseAuthServiceTest {

    @Test
    void registrationUsesGeneratedDatabaseKeyAndStoresHashedPassword() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class)))
                .thenAnswer(invocation -> {
                    KeyHolder keyHolder = invocation.getArgument(1);
                    keyHolder.getKeyList().add(Map.of("GENERATED_KEY", 98765L));
                    return 1;
                });
        DatabaseAuthService service = new DatabaseAuthService(
                jdbcTemplate,
                mock(JavaMailSender.class),
                new PasswordResetTokenService("unit-test-reset-secret-at-least-32-chars"),
                "noreply@example.com");

        var user = service.registerStudent("  New Student  ", "NEW@EXAMPLE.COM", "a-long-test-password");

        assertThat(user.getUserId()).isEqualTo(98765L);
        assertThat(user.getFullName()).isEqualTo("New Student");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getRole()).isEqualTo("student");
        assertThat(user.getPasswordHash()).isNotEqualTo("a-long-test-password");
    }
}