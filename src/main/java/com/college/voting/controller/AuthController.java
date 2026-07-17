package com.college.voting.controller;

import com.college.voting.dto.LoginRequest;
import com.college.voting.dto.OtpRequest;
import com.college.voting.entity.Student;
import com.college.voting.repository.StudentRepository;
import com.college.voting.service.AuditLogService;
import com.college.voting.service.OtpService;
import com.college.voting.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final StudentRepository studentRepository;
    private final OtpService otpService;
    private final RateLimitService rateLimitService;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          StudentRepository studentRepository,
                          OtpService otpService,
                          RateLimitService rateLimitService,
                          AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.studentRepository = studentRepository;
        this.otpService = otpService;
        this.rateLimitService = rateLimitService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> studentLogin(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername();
        String ip = request.getRemoteAddr();

        // 1. Check rate limits
        if (rateLimitService.isLoginBlocked(username, ip)) {
            auditLogService.log(username, "Login Blocked - Too many attempts", request);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Rate limit exceeded", "message", "Too many failed attempts. Account locked for 15 minutes."));
        }

        try {
            // 2. Authenticate
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            // 3. Set security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 4. Fetch student details
            Optional<Student> studentOpt = studentRepository.findByRegisterNo(username);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Student record not found."));
            }
            Student student = studentOpt.get();

            // 5. Generate and dispatch OTP
            otpService.generateAndSendOtp(student);
            
            // 6. Reset rate limits & set session attributes
            rateLimitService.loginSucceeded(username, ip);
            HttpSession session = request.getSession(true);
            session.setAttribute("OTP_VERIFIED", false);
            session.setAttribute("STUDENT_ID", student.getId());
            session.setAttribute("REGISTER_NO", student.getRegisterNo());

            // Persist SecurityContext for Spring Security 6
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            auditLogService.log(username, "Student login initiated - OTP dispatched", request);

            return ResponseEntity.ok(Map.of("message", "Login successful. OTP sent to your registered email."));

        } catch (BadCredentialsException e) {
            rateLimitService.loginFailed(username, ip);
            auditLogService.log(username, "Failed login attempt", request);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized", "message", "Invalid register number or password."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error", "message", e.getMessage() != null ? e.getMessage() : "Internal Server Error"));
        }
    }

    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername();
        String ip = request.getRemoteAddr();

        if (rateLimitService.isLoginBlocked(username, ip)) {
            auditLogService.log(username, "Admin Login Blocked - Too many attempts", request);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Rate limit exceeded", "message", "Too many failed attempts. Account locked for 15 minutes."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            rateLimitService.loginSucceeded(username, ip);
            
            HttpSession session = request.getSession(true);
            session.setAttribute("OTP_VERIFIED", true); // Admin bypasses OTP

            // Persist SecurityContext for Spring Security 6
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            auditLogService.log(username, "Admin login successful", request);
            return ResponseEntity.ok(Map.of("message", "Admin login successful."));

        } catch (BadCredentialsException e) {
            rateLimitService.loginFailed(username, ip);
            auditLogService.log(username, "Failed Admin login attempt", request);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized", "message", "Invalid admin credentials."));
        }
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest otpRequest, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        String registerNo = auth.getName();
        Optional<Student> studentOpt = studentRepository.findByRegisterNo(registerNo);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Student not found"));
        }

        Student student = studentOpt.get();

        try {
            boolean isVerified = otpService.verifyOtp(student, otpRequest.getOtp());
            if (isVerified) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.setAttribute("OTP_VERIFIED", true);
                }
                rateLimitService.resetOtpRequests(student.getId());
                auditLogService.log(registerNo, "OTP verified successfully", request);
                return ResponseEntity.ok(Map.of("message", "OTP verified successfully."));
            } else {
                auditLogService.log(registerNo, "OTP mismatch", request);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid OTP code. Please try again."));
            }
        } catch (Exception e) {
            auditLogService.log(registerNo, "OTP Verification failed: " + e.getMessage(), request);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/otp/resend")
    public ResponseEntity<?> resendOtp(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        String registerNo = auth.getName();
        Optional<Student> studentOpt = studentRepository.findByRegisterNo(registerNo);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Student not found"));
        }

        Student student = studentOpt.get();

        String blockReason = rateLimitService.getOtpBlockReason(student.getId());
        if (blockReason != null) {
            auditLogService.log(registerNo, "OTP Resend blocked: " + blockReason, request);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too Many Requests", "message", blockReason));
        }

        try {
            otpService.generateAndSendOtp(student);
            rateLimitService.recordOtpRequest(student.getId());
            auditLogService.log(registerNo, "OTP resent", request);
            return ResponseEntity.ok(Map.of("message", "OTP resent successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to resend OTP"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }

        Map<String, Object> status = new HashMap<>();
        status.put("loggedIn", true);
        status.put("username", auth.getName());

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        status.put("role", isAdmin ? "ADMIN" : "STUDENT");

        HttpSession session = request.getSession(false);
        boolean isOtpVerified = session != null && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"));
        status.put("otpVerified", isOtpVerified);

        if (!isAdmin) {
            Optional<Student> studentOpt = studentRepository.findByRegisterNo(auth.getName());
            if (studentOpt.isPresent()) {
                Student s = studentOpt.get();
                Map<String, Object> details = new HashMap<>();
                details.put("id", s.getId());
                details.put("registerNo", s.getRegisterNo());
                details.put("name", s.getName());
                details.put("department", s.getDepartment());
                details.put("year", s.getYear());
                details.put("email", s.getEmail());
                details.put("photo", s.getPhoto());
                status.put("student", details);
            }
        }

        return ResponseEntity.ok(status);
    }
}
