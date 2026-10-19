package com.openstudy.openstudy.service;

import com.openstudy.openstudy.model.User;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockAuthService implements AuthService {

    private static final Map<String, User> USERS = Map.of(
            "student@example.com", new User(1001L, "Student User", "student@example.com", "mock-student", "student", "ACTIVE"),
            "teacher@example.com", new User(2001L, "Teacher User", "teacher@example.com", "mock-teacher", "teacher", "ACTIVE")
    );

    @Override
    public User authenticate(String email, String password, String role) {
        if (email == null || password == null || role == null) {
            return null;
        }

        User user = USERS.get(email.trim().toLowerCase());
        if (user == null) {
            return null;
        }

        String expectedPassword = switch (role.trim().toLowerCase()) {
            case "student" -> "student123";
            case "teacher" -> "teacher123";
            default -> null;
        };
        if (expectedPassword == null || !expectedPassword.equals(password)
                || !user.getRole().equalsIgnoreCase(role.trim())) {
            return null;
        }

        return new User(user.getUserId(), user.getFullName(), user.getEmail(), user.getPasswordHash(),
                user.getRole(), user.getAccountStatus());
    }

    @Override
    public User registerStudent(String fullName, String email, String password) {
        throw new UnsupportedOperationException("Student sign-up requires the database profile.");
    }

    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        throw new UnsupportedOperationException("Password reset requires database and email configuration.");
    }

    @Override
    public boolean isResetTokenValid(String token) {
        return false;
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        return false;
    }
}