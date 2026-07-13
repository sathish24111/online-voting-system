package com.college.voting.service;

import com.college.voting.entity.Student;
import com.college.voting.repository.StudentRepository;
import com.college.voting.utils.PasswordValidator;
import com.college.voting.utils.StudentImportHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<Student> getStudents(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return studentRepository.findByNameContainingIgnoreCaseOrRegisterNoContainingIgnoreCase(
                search, search, pageable
            );
        }
        return studentRepository.findAll(pageable);
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public Student addStudent(Student student) {
        // Validation
        if (studentRepository.existsByRegisterNo(student.getRegisterNo())) {
            throw new RuntimeException("Register Number '" + student.getRegisterNo() + "' is already registered.");
        }
        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new RuntimeException("Email address '" + student.getEmail() + "' is already registered.");
        }
        if (!PasswordValidator.isValid(student.getPassword())) {
            throw new RuntimeException("Password does not meet complexity requirements. It must contain at least 8 characters, including uppercase, lowercase, numbers, and special characters.");
        }

        student.setRegisterNo(student.getRegisterNo().toUpperCase().trim());
        student.setEmail(student.getEmail().toLowerCase().trim());
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student details) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Student not found."));

        // Check uniqueness if modified
        if (!student.getRegisterNo().equalsIgnoreCase(details.getRegisterNo()) 
            && studentRepository.existsByRegisterNo(details.getRegisterNo())) {
            throw new RuntimeException("Register Number is already in use.");
        }
        if (!student.getEmail().equalsIgnoreCase(details.getEmail()) 
            && studentRepository.existsByEmail(details.getEmail())) {
            throw new RuntimeException("Email address is already in use.");
        }

        student.setName(details.getName());
        student.setRegisterNo(details.getRegisterNo().toUpperCase().trim());
        student.setDepartment(details.getDepartment());
        student.setYear(details.getYear());
        student.setEmail(details.getEmail().toLowerCase().trim());
        student.setDob(details.getDob());
        
        if (details.getPhoto() != null) {
            student.setPhoto(details.getPhoto());
        }

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found.");
        }
        studentRepository.deleteById(id);
    }

    @Transactional
    public Student toggleStatus(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Student not found."));
        
        if ("ENABLED".equalsIgnoreCase(student.getStatus())) {
            student.setStatus("DISABLED");
        } else {
            student.setStatus("ENABLED");
        }
        return studentRepository.save(student);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Student not found."));

        if (!PasswordValidator.isValid(newPassword)) {
            throw new RuntimeException("Password must contain at least 8 characters, including uppercase, lowercase, numbers, and special characters.");
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        studentRepository.save(student);
    }

    @Transactional
    public Map<String, Object> importStudents(MultipartFile file) {
        StudentImportHelper.ImportResult importResult = StudentImportHelper.importStudents(file, passwordEncoder);
        
        List<Student> toSave = new ArrayList<>();
        int duplicates = 0;
        List<String> importLogs = new ArrayList<>(importResult.getLogs());

        for (Student student : importResult.getStudents()) {
            boolean regExists = studentRepository.existsByRegisterNo(student.getRegisterNo());
            boolean emailExists = studentRepository.existsByEmail(student.getEmail());

            if (regExists || emailExists) {
                duplicates++;
                importLogs.add("Row " + (toSave.size() + duplicates + 1) + ": Skipped duplicate. Register Number (" + student.getRegisterNo() + ") or Email (" + student.getEmail() + ") already exists.");
            } else {
                toSave.add(student);
            }
        }

        if (!toSave.isEmpty()) {
            studentRepository.saveAll(toSave);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRows", importResult.getTotalRows());
        summary.put("successCount", toSave.size());
        summary.put("duplicateCount", duplicates);
        summary.put("errorCount", importResult.getErrorCount() + (importResult.getStudents().size() - toSave.size() - duplicates));
        summary.put("logs", importLogs);

        return summary;
    }
}
