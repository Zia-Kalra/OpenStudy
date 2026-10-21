package com.openstudy.openstudy.service;

import com.openstudy.openstudy.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetTokenService {

    private static final long TOKEN_LIFETIME_SECONDS = 30 * 60;
    private final byte[] secret;

    public PasswordResetTokenService(@Value("${openstudy.auth.reset-secret:}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String createToken(User user) {
        requireSecret();
        long expiresAt = System.currentTimeMillis() / 1000 + TOKEN_LIFETIME_SECONDS;
        String passwordDigest = encode(digest(user.getPasswordHash()));
        String payload = user.getUserId() + "." + expiresAt + "." + passwordDigest;
        return encode(payload.getBytes(StandardCharsets.UTF_8)) + "." + encode(sign(payload));
    }

    public Claims parse(String token) {
        if (token == null || secret.length < 32) {
            return null;
        }
        try {
            String[] tokenParts = token.split("\\.", 2);
            if (tokenParts.length != 2) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(payload), Base64.getUrlDecoder().decode(tokenParts[1]))) {
                return null;
            }
            String[] fields = payload.split("\\.", 3);
            if (fields.length != 3) {
                return null;
            }
            long userId = Long.parseLong(fields[0]);
            long expiresAt = Long.parseLong(fields[1]);
            if (expiresAt <= System.currentTimeMillis() / 1000) {
                return null;
            }
            return new Claims(userId, fields[2]);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public boolean matchesPassword(Claims claims, String passwordHash) {
        if (claims == null || passwordHash == null) {
            return false;
        }
        return MessageDigest.isEqual(claims.passwordDigest().getBytes(StandardCharsets.US_ASCII),
                encode(digest(passwordHash)).getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to sign password reset token.", exception);
        }
    }

    private byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash password reset token data.", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private void requireSecret() {
        if (secret.length < 32) {
            throw new IllegalStateException("PASSWORD_RESET_SECRET must contain at least 32 characters.");
        }
    }

    public record Claims(long userId, String passwordDigest) {
    }
}