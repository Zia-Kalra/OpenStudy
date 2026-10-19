package com.openstudy.openstudy.service;

import com.openstudy.openstudy.model.User;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Profile("!mock")
public class DatabaseAuthService implements AuthService {

    private static final String USER_COLUMNS = "user_id, full_name, email, password_hash, role, account_status";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JavaMailSender mailSender;
    private final PasswordResetTokenService resetTokens;
    private final String fromAddress;

    public DatabaseAuthService(
            JdbcTemplate jdbcTemplate,
            JavaMailSender mailSender,
            PasswordResetTokenService resetTokens,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
        this.resetTokens = resetTokens;
        this.fromAddress = fromAddress;
    }

    @Override
    public User authenticate(String email, String password, String role) {
        if (email == null || password == null || role == null) {
            return null;
        }
        User user = queryUser(
                "SELECT " + USER_COLUMNS + " FROM users WHERE email = ? AND account_status = 'ACTIVE'",
                normalizeEmail(email));
        if (user == null) {
            return null;
        }
        if (!user.getRole().equalsIgnoreCase(role.trim()) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return null;
        }
        return user;
    }

    @Override
    public User registerStudent(String fullName, String email, String password) {
        String normalizedName = fullName.trim();
        String normalizedEmail = normalizeEmail(email);
        String encodedPassword = passwordEncoder.encode(password);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator statement = connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO users (full_name, email, password_hash, role, account_status) "
                            + "VALUES (?, ?, ?, 'STUDENT', 'ACTIVE')",
                    Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, normalizedName);
            preparedStatement.setString(2, normalizedEmail);
            preparedStatement.setString(3, encodedPassword);
            return preparedStatement;
        };
        try {
            jdbcTemplate.update(statement, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("An account with that email already exists.", exception);
        }
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("The database did not return a generated account ID.");
        }
        return new User(generatedId.longValue(), normalizedName, normalizedEmail, encodedPassword, "student", "ACTIVE");
    }

    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        User user = queryUser(
                "SELECT " + USER_COLUMNS + " FROM users WHERE email = ? AND account_status = 'ACTIVE'",
                normalizeEmail(email));
        if (user == null) {
            return;
        }

        String token = resetTokens.createToken(user);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Reset your OpenStudy password");
        message.setText("Use this link within 30 minutes to reset your password:\n\n"
                + resetUrl + "?token=" + token + "\n\nIf you did not request this, ignore this email.");
        mailSender.send(message);
    }

    @Override
    public boolean isResetTokenValid(String token) {
        PasswordResetTokenService.Claims claims = resetTokens.parse(token);
        User user = claims == null ? null : findById(claims.userId());
        return user != null && "ACTIVE".equalsIgnoreCase(user.getAccountStatus())
                && resetTokens.matchesPassword(claims, user.getPasswordHash());
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetTokenService.Claims claims = resetTokens.parse(token);
        User user = claims == null ? null : findById(claims.userId());
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getAccountStatus())
                || !resetTokens.matchesPassword(claims, user.getPasswordHash())) {
            return false;
        }
        return jdbcTemplate.update(
                "UPDATE users SET password_hash = ? WHERE user_id = ? AND password_hash = ?",
                passwordEncoder.encode(newPassword), user.getUserId(), user.getPasswordHash()) == 1;
    }

    private User findById(long userId) {
        return queryUser(
                "SELECT " + USER_COLUMNS + " FROM users WHERE user_id = ?",
                userId);
    }

    private User queryUser(String sql, Object... arguments) {
        return jdbcTemplate.query(sql, resultSet -> resultSet.next() ? mapUser(resultSet) : null, arguments);
    }

    private User mapUser(ResultSet resultSet) throws java.sql.SQLException {
        return new User(resultSet.getLong("user_id"), resultSet.getString("full_name"),
                resultSet.getString("email"), resultSet.getString("password_hash"),
                resultSet.getString("role").toLowerCase(Locale.ROOT), resultSet.getString("account_status"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}