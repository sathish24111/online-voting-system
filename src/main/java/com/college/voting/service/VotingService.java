package com.college.voting.service;

import com.college.voting.entity.*;
import com.college.voting.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final StudentRepository studentRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionParticipationRepository participationRepository;

    public VotingService(VoteRepository voteRepository,
                         StudentRepository studentRepository,
                         ElectionRepository electionRepository,
                         CandidateRepository candidateRepository,
                         ElectionParticipationRepository participationRepository) {
        this.voteRepository = voteRepository;
        this.studentRepository = studentRepository;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.participationRepository = participationRepository;
    }

    @Transactional
    public void castBallot(Long studentId, Long electionId, List<Long> candidateIds) {
        // 1. Verify election exists and is currently RUNNING
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found."));

        if (!"RUNNING".equalsIgnoreCase(election.getStatus())) {
            throw new RuntimeException("Voting is not active for this election. Current status: " + election.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(election.getStartTime()) || now.isAfter(election.getEndTime())) {
            // Automatically close election if time exceeded
            if (now.isAfter(election.getEndTime())) {
                election.setStatus("ENDED");
                electionRepository.save(election);
            }
            throw new RuntimeException("Election voting period has ended or not yet started.");
        }

        // 2. Verify student exists and is enabled
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found."));

        if (!"ENABLED".equalsIgnoreCase(student.getStatus())) {
            throw new RuntimeException("Your student account is currently disabled. Please contact the administrator.");
        }

        // 3. Verify student has not already voted in this election
        boolean alreadyVoted = participationRepository.existsByStudentIdAndElectionId(studentId, electionId);
        if (alreadyVoted) {
            throw new RuntimeException("You have already voted in this election.");
        }

        // 4. Validate candidate selections (check existence, correct election, and ensure max 1 vote per position)
        Set<String> votedPositions = new HashSet<>();
        List<Vote> votesToSave = new ArrayList<>();

        for (Long candidateId : candidateIds) {
            Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Selected candidate ID " + candidateId + " not found."));

            if (!candidate.getElection().getId().equals(electionId)) {
                throw new RuntimeException("Candidate '" + candidate.getName() + "' is not registered in this election.");
            }

            String position = candidate.getPosition();
            if (votedPositions.contains(position)) {
                throw new RuntimeException("Duplicate votes detected for position: " + position + ". You can only vote for one candidate per position.");
            }

            votedPositions.add(position);
            
            // Build anonymous vote row
            votesToSave.add(new Vote(election, candidate, position, student));
        }

        // 5. Record Student Participation
        ElectionParticipation participation = new ElectionParticipation(student, election);
        participationRepository.save(participation);

        // 6. Save Anonymous Votes
        voteRepository.saveAll(votesToSave);
    }
}
