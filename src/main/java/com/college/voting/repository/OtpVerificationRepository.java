package com.college.voting.repository;

import com.college.voting.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findByStudentIdAndVerifiedFalseAndExpiryTimeAfter(
        Long studentId, LocalDateTime currentTime
    );
    void deleteByStudentId(Long studentId);
}
