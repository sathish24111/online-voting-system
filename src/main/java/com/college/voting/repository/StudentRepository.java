package com.college.voting.repository;

import com.college.voting.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRegisterNo(String registerNo);
    Optional<Student> findByEmail(String email);
    boolean existsByRegisterNo(String registerNo);
    boolean existsByEmail(String email);
    
    Page<Student> findByNameContainingIgnoreCaseOrRegisterNoContainingIgnoreCase(
        String name, String registerNo, Pageable pageable
    );
}
