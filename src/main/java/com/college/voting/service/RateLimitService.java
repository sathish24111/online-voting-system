package com.college.voting.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.Duration;
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
    
    private static final int MAX_OTP_REQUESTS = 2;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    /**
     * Checks if the username is currently blocked due to failed login attempts.
     */
    public boolean isLoginBlocked(String username, String ip) {
        return false; // Disable lockout completely as requested
    }

    private boolean checkBlocked(String key) {
        return false;
    }

    /**
     * Records a failed login attempt for the username.
     */
    public void loginFailed(String username, String ip) {
        recordFailure(username);
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
    }

    /**
     * Gets the human-readable block reason if a student is throttled or locked out.
     * Returns null if they are not blocked.
     */
    public String getOtpBlockReason(Long studentId) {
        OtpAttempt attempt = otpAttempts.get(studentId);
        if (attempt == null) {
            return null;
        }
        
        // 1. Cooldown period (60 seconds)
        if (LocalDateTime.now().isBefore(attempt.lastRequestTime.plusSeconds(OTP_RESEND_COOLDOWN_SECONDS))) {
            long remainingSecs = OTP_RESEND_COOLDOWN_SECONDS - Duration.between(attempt.lastRequestTime, LocalDateTime.now()).toSeconds();
            return "OTP resend is on cooldown. Please wait " + Math.max(1, remainingSecs) + " seconds.";
        }
        
        // 2. Max limit (2 resends)
        if (attempt.count >= MAX_OTP_REQUESTS) {
            // Block further resends for 5 minutes
            if (LocalDateTime.now().isAfter(attempt.lastRequestTime.plusMinutes(5))) {
                return null; // 5 minutes have passed, block expired
            }
            long remainingSecs = 300 - Duration.between(attempt.lastRequestTime, LocalDateTime.now()).toSeconds();
            long remainingMins = (remainingSecs + 59) / 60;
            return "You have exceeded the maximum of 2 OTP resends. Further resends are blocked for " + Math.max(1, remainingMins) + " minutes, or until an administrator resets it.";
        }
        
        return null;
    }

    /**
     * Checks if the student is blocked from requesting a new OTP.
     */
    public boolean isOtpRequestBlocked(Long studentId) {
        return getOtpBlockReason(studentId) != null;
    }

    /**
     * Records an OTP request.
     */
    public void recordOtpRequest(Long studentId) {
        otpAttempts.compute(studentId, (k, v) -> {
            if (v == null) {
                return new OtpAttempt(1, LocalDateTime.now());
            }
            // If the 5-minute block has expired, reset count to 1
            if (v.count >= MAX_OTP_REQUESTS && LocalDateTime.now().isAfter(v.lastRequestTime.plusMinutes(5))) {
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
