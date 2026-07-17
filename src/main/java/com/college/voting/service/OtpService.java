package com.college.voting.service;

import com.college.voting.entity.OtpVerification;
import com.college.voting.entity.Student;
import com.college.voting.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpVerificationRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void generateAndSendOtp(Student student) {
        // Delete existing OTPs
        otpRepository.deleteByStudentId(student.getId());

        // Generate 6-digit code
        String otp = String.format("%06d", random.nextInt(1000000));

        // Expiry set to 5 minutes
        OtpVerification verification = new OtpVerification(student, otp, 5);
        otpRepository.save(verification);

        // Dispatch email
        emailService.sendOtpEmail(student.getEmail(), student.getName(), otp);
    }

    @Transactional
    public boolean verifyOtp(Student student, String code) {
        Optional<OtpVerification> optVerification = otpRepository
            .findByStudentIdAndVerifiedFalseAndExpiryTimeAfter(student.getId(), LocalDateTime.now());

        if (optVerification.isEmpty()) {
            return false;
        }

        OtpVerification verification = optVerification.get();

        if (verification.getAttempts() >= 5) {
            otpRepository.delete(verification);
            throw new RuntimeException("Maximum OTP attempts exceeded. Please login again.");
        }

        if (verification.getOtp().equals(code)) {
            // Delete OTP after verification for security
            otpRepository.deleteByStudentId(student.getId());
            return true;
        } else {
            verification.setAttempts(verification.getAttempts() + 1);
            if (verification.getAttempts() >= 5) {
                otpRepository.delete(verification);
                throw new RuntimeException("Maximum OTP attempts exceeded. Please login again.");
            } else {
                otpRepository.save(verification);
            }
            return false;
        }
    }
}
