package com.college.voting.controller;

import com.college.voting.entity.Student;
import com.college.voting.service.AuditLogService;
import com.college.voting.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentController {

    private final StudentService studentService;
    private final AuditLogService auditLogService;

    public AdminStudentController(StudentService studentService, AuditLogService auditLogService) {
        this.studentService = studentService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Page<Student>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(studentService.getStudents(search, pageable));
    }

    @PostMapping
    public ResponseEntity<?> addStudent(@RequestBody Student student, HttpServletRequest request, Principal principal) {
        try {
            Student saved = studentService.addStudent(student);
            auditLogService.log(principal.getName(), "Created Student: " + saved.getRegisterNo(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @RequestBody Student student, HttpServletRequest request, Principal principal) {
        try {
            Student updated = studentService.updateStudent(id, student);
            auditLogService.log(principal.getName(), "Updated Student ID: " + id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id, HttpServletRequest request, Principal principal) {
        try {
            studentService.deleteStudent(id);
            auditLogService.log(principal.getName(), "Deleted Student ID: " + id, request);
            return ResponseEntity.ok(Map.of("message", "Student deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllStudents(HttpServletRequest request, Principal principal) {
        try {
            studentService.deleteAllStudents();
            auditLogService.log(principal.getName(), "Deleted ALL Students and voting data", request);
            return ResponseEntity.ok(Map.of("message", "All student and voting records cleared successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id, HttpServletRequest request, Principal principal) {
        try {
            Student updated = studentService.toggleStatus(id);
            auditLogService.log(principal.getName(), "Toggled Student " + updated.getRegisterNo() + " status to " + updated.getStatus(), request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request, Principal principal) {
        try {
            String newPassword = body.get("password");
            studentService.resetPassword(id, newPassword);
            auditLogService.log(principal.getName(), "Reset Password for Student ID: " + id, request);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importStudents(@RequestParam("file") MultipartFile file, HttpServletRequest request, Principal principal) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Uploaded file is empty."));
        }
        try {
            Map<String, Object> summary = studentService.importStudents(file);
            auditLogService.log(principal.getName(), "Imported students list. Success count: " + summary.get("successCount"), request);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error importing students: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam(defaultValue = "xlsx") String format) {
        byte[] content;
        String filename;
        MediaType mediaType;

        if ("csv".equalsIgnoreCase(format)) {
            String csv = "Register Number,Student Name,Department,Year,Email\n" +
                          "2026CSE001,John Doe,Computer Science,III Year,johndoe@gmail.com\n" +
                          "2026ECE002,Jane Smith,Electronics & Comm,IV Year,janesmith@gmail.com\n";
            content = csv.getBytes(StandardCharsets.UTF_8);
            filename = "students_import_template.csv";
            mediaType = MediaType.parseMediaType("text/csv");
        } else {
            // Very simple binary Excel mockup or raw Excel spreadsheet skeleton structure
            // Let's create an empty workbook or a basic valid xlsx file.
            // A simple CSV works perfectly, but for Excel template: we can construct a minimal workbook with header cells using POI
            try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                org.apache.poi.ss.usermodel.Sheet sh = wb.createSheet("Students Template");
                org.apache.poi.ss.usermodel.Row row = sh.createRow(0);
                row.createCell(0).setCellValue("Register Number");
                row.createCell(1).setCellValue("Student Name");
                row.createCell(2).setCellValue("Department");
                row.createCell(3).setCellValue("Year");
                row.createCell(4).setCellValue("Email");
                
                org.apache.poi.ss.usermodel.Row sample = sh.createRow(1);
                sample.createCell(0).setCellValue("2026CSE001");
                sample.createCell(1).setCellValue("John Doe");
                sample.createCell(2).setCellValue("Computer Science");
                sample.createCell(3).setCellValue("III Year");
                sample.createCell(4).setCellValue("johndoe@gmail.com");

                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                wb.write(out);
                content = out.toByteArray();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            filename = "students_import_template.xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentType(mediaType);
        
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
