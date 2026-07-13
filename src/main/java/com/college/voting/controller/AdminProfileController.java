package com.college.voting.controller;

import com.college.voting.entity.Admin;
import com.college.voting.repository.AdminRepository;
import com.college.voting.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminProfileController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminProfileController(AdminRepository adminRepository,
                                  PasswordEncoder passwordEncoder,
                                  AuditLogService auditLogService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request, Principal principal) {
        String username = principal.getName();
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);

        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Admin record not found."));
        }

        try {
            String newPassword = body.get("password");
            if (newPassword == null || newPassword.length() < 8) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Password must contain at least 8 characters."));
            }

            Admin admin = adminOpt.get();
            admin.setPassword(passwordEncoder.encode(newPassword));
            adminRepository.save(admin);

            auditLogService.log(username, "Admin changed password", request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
