package com.openstudy.openstudy.service;

import com.openstudy.openstudy.model.User;

public interface AuthService {

    User authenticate(String email, String password, String role);

    User registerStudent(String fullName, String email, String password);

    void sendPasswordReset(String email, String resetUrl);

    boolean isResetTokenValid(String token);

    boolean resetPassword(String token, String newPassword);
}
