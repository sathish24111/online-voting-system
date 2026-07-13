package com.college.voting.repository;

import com.college.voting.entity.ElectionParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElectionParticipationRepository extends JpaRepository<ElectionParticipation, Long> {
    boolean existsByStudentIdAndElectionId(Long studentId, Long electionId);
    long countByElectionId(Long electionId);
}
