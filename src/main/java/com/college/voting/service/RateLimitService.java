package com.college.voting.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static class LoginAttempt {
        int attempts;
        LocalDateTime lastFailedTime;

        LoginAttempt(int attempts, LocalDateTime lastFailedTime) {
            this.attempts = attempts;
            this.lastFailedTime = lastFailedTime;
        }
    }

    private static class OtpAttempt {
        int count;
        LocalDateTime lastRequestTime;

        OtpAttempt(int count, LocalDateTime lastRequestTime) {
            this.count = count;
            this.lastRequestTime = lastRequestTime;
        }
    }

    // Key: Username/RegisterNo or IP Address
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    
    // Key: Student ID
    private final Map<Long, OtpAttempt> otpAttempts = new ConcurrentHashMap<>();

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_BLOCK_MINUTES = 15;
    
    private static final int MAX_OTP_REQUESTS = 3;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 30;

    /**
     * Checks if the username or IP is currently blocked due to failed login attempts.
     */
    public boolean isLoginBlocked(String username, String ip) {
        return checkBlocked(username) || checkBlocked(ip);
    }

    private boolean checkBlocked(String key) {
        LoginAttempt attempt = loginAttempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.attempts >= MAX_LOGIN_ATTEMPTS) {
            if (LocalDateTime.now().isBefore(attempt.lastFailedTime.plusMinutes(LOGIN_BLOCK_MINUTES))) {
                return true;
            } else {
                // Block expired, reset attempts
                loginAttempts.remove(key);
            }
        }
        return false;
    }

    /**
     * Records a failed login attempt for both username and IP.
     */
    public void loginFailed(String username, String ip) {
        recordFailure(username);
        recordFailure(ip);
    }

    private void recordFailure(String key) {
        loginAttempts.compute(key, (k, v) -> {
            if (v == null) {
                return new LoginAttempt(1, LocalDateTime.now());
            }
            // If previous block expired, reset count to 1
            if (LocalDateTime.now().isAfter(v.lastFailedTime.plusMinutes(LOGIN_BLOCK_MINUTES))) {
                return new LoginAttempt(1, LocalDateTime.now());
            }
            v.attempts++;
            v.lastFailedTime = LocalDateTime.now();
            return v;
        });
    }

    /**
     * Clears failed login attempts for a successful login.
     */
    public void loginSucceeded(String username, String ip) {
        loginAttempts.remove(username);
        loginAttempts.remove(ip);
    }

    /**
     * Checks if the student is blocked from requesting a new OTP (cooldown or max requests).
     */
    public boolean isOtpRequestBlocked(Long studentId) {
        OtpAttempt attempt = otpAttempts.get(studentId);
        if (attempt == null) {
            return false;
        }
        
        // Cooldown period (30 seconds)
        if (LocalDateTime.now().isBefore(attempt.lastRequestTime.plusSeconds(OTP_RESEND_COOLDOWN_SECONDS))) {
            return true;
        }
        
        // Max limit (3 resends)
        return attempt.count >= MAX_OTP_REQUESTS;
    }

    /**
     * Records an OTP request.
     */
    public void recordOtpRequest(Long studentId) {
        otpAttempts.compute(studentId, (k, v) -> {
            if (v == null) {
                return new OtpAttempt(1, LocalDateTime.now());
            }
            v.count++;
            v.lastRequestTime = LocalDateTime.now();
            return v;
        });
    }

    /**
     * Resets OTP request counts (e.g. after successful login or session end).
     */
    public void resetOtpRequests(Long studentId) {
        otpAttempts.remove(studentId);
    }
}
