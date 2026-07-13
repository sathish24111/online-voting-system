package com.college.voting.repository;

import com.college.voting.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByElectionId(Long electionId);
    List<Candidate> findByElectionIdAndPosition(Long electionId, String position);
    List<Candidate> findByElectionIdAndNameContainingIgnoreCase(Long electionId, String name);
    boolean existsByElectionIdAndRegisterNo(Long electionId, String registerNo);
}
