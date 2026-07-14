package com.college.voting.repository;

import com.college.voting.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    long countByElectionId(Long electionId);
    
    @Query("SELECT v.candidate.id AS candidateId, COUNT(v) AS voteCount FROM Vote v " +
           "WHERE v.election.id = :electionId GROUP BY v.candidate.id")
    List<Object[]> countVotesGroupByCandidate(@Param("electionId") Long electionId);

    @Query("SELECT v.position AS position, COUNT(v) AS voteCount FROM Vote v " +
           "WHERE v.election.id = :electionId GROUP BY v.position")
    List<Object[]> countVotesGroupByPosition(@Param("electionId") Long electionId);
    
    List<Vote> findByElectionId(Long electionId);

    @Query("SELECT v FROM Vote v LEFT JOIN FETCH v.student LEFT JOIN FETCH v.candidate WHERE v.election.id = :electionId")
    List<Vote> findVotesWithStudentAndCandidateByElectionId(@Param("electionId") Long electionId);
}
