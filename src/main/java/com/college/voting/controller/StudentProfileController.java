package com.college.voting.controller;

import com.college.voting.entity.Student;
import com.college.voting.repository.StudentRepository;
import com.college.voting.service.AuditLogService;
import com.college.voting.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/student")
public class StudentProfileController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;

    public StudentProfileController(StudentService studentService,
                                    StudentRepository studentRepository,
                                    AuditLogService auditLogService) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request, Principal principal) {
        String username = principal.getName();
        Optional<Student> studentOpt = studentRepository.findByRegisterNo(username);
        
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Student record not found."));
        }

        try {
            String newPassword = body.get("password");
            Student student = studentOpt.get();
            studentService.resetPassword(student.getId(), newPassword);
            auditLogService.log(username, "Student changed their password", request);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
