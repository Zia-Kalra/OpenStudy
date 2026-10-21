package com.openstudy.openstudy.service;

import com.openstudy.openstudy.model.User;

public interface AuthService {

    User authenticate(String email, String password, String role);

    User registerStudent(String fullName, String email, String password);

    default User registerUser(String fullName, String email, String password, String role) {
        if (role != null && "student".equalsIgnoreCase(role.trim())) {
            return registerStudent(fullName, email, password);
        }
        throw new UnsupportedOperationException("This authentication provider does not support teacher registration.");
    }

    void sendPasswordReset(String email, String resetUrl);

    boolean isResetTokenValid(String token);

    boolean resetPassword(String token, String newPassword);
}
