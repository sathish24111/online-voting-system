package com.college.voting.controller;

import com.college.voting.dto.BallotRequest;
import com.college.voting.entity.Candidate;
import com.college.voting.entity.Election;
import com.college.voting.entity.Student;
import com.college.voting.repository.ElectionParticipationRepository;
import com.college.voting.repository.StudentRepository;
import com.college.voting.service.AuditLogService;
import com.college.voting.service.CandidateService;
import com.college.voting.service.ElectionService;
import com.college.voting.service.VotingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/voting")
public class StudentVotingController {

    private final VotingService votingService;
    private final ElectionService electionService;
    private final CandidateService candidateService;
    private final StudentRepository studentRepository;
    private final ElectionParticipationRepository participationRepository;
    private final AuditLogService auditLogService;

    public StudentVotingController(VotingService votingService,
                                   ElectionService electionService,
                                   CandidateService candidateService,
                                   StudentRepository studentRepository,
                                   ElectionParticipationRepository participationRepository,
                                   AuditLogService auditLogService) {
        this.votingService = votingService;
        this.electionService = electionService;
        this.candidateService = candidateService;
        this.studentRepository = studentRepository;
        this.participationRepository = participationRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/active-election")
    public ResponseEntity<?> getActiveElection(Principal principal) {
        Optional<Election> electionOpt = electionService.getActiveElection();
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No active election currently running."));
        }

        Election election = electionOpt.get();
        Optional<Student> studentOpt = studentRepository.findByRegisterNo(principal.getName());
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Student profile not found."));
        }

        Student student = studentOpt.get();
        boolean hasVoted = participationRepository.existsByStudentIdAndElectionId(student.getId(), election.getId());
        List<Candidate> candidates = candidateService.getCandidatesByElection(election.getId());

        // Format candidate responses to hide relationship cycles
        // Grouping logic can be handled on the frontend easily or structured here
        Map<String, Object> response = new HashMap<>();
        response.put("election", election);
        response.put("hasVoted", hasVoted);
        response.put("candidates", candidates);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cast")
    public ResponseEntity<?> castBallot(@RequestBody BallotRequest ballotRequest, HttpServletRequest request, Principal principal) {
        Optional<Student> studentOpt = studentRepository.findByRegisterNo(principal.getName());
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Student profile not found."));
        }

        Student student = studentOpt.get();
        String regNo = student.getRegisterNo();

        try {
            votingService.castBallot(student.getId(), ballotRequest.getElectionId(), ballotRequest.getCandidateIds());
            
            // Invalidate session immediately after casting ballot to prevent double submit or navigation back
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            auditLogService.log(regNo, "Cast Ballot successfully - Session terminated", request);
            
            return ResponseEntity.ok(Map.of("message", "Thank you. Your vote has been recorded successfully."));
        } catch (Exception e) {
            auditLogService.log(regNo, "Failed to cast ballot: " + e.getMessage(), request);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
