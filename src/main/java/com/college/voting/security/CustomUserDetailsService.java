package com.college.voting.security;

import com.college.voting.entity.Admin;
import com.college.voting.entity.Student;
import com.college.voting.repository.AdminRepository;
import com.college.voting.repository.StudentRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;

    public CustomUserDetailsService(AdminRepository adminRepository, StudentRepository studentRepository) {
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Try to load as Admin
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            return new User(
                admin.get().getUsername(),
                admin.get().getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(admin.get().getRole()))
            );
        }

        // 2. Try to load as Student
        Optional<Student> student = studentRepository.findByRegisterNo(username);
        if (student.isPresent()) {
            if (!"ENABLED".equalsIgnoreCase(student.get().getStatus())) {
                throw new UsernameNotFoundException("Student account is disabled: " + username);
            }
            return new User(
                student.get().getRegisterNo(),
                student.get().getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
            );
        }

        throw new UsernameNotFoundException("User not found with username/register number: " + username);
    }
}
