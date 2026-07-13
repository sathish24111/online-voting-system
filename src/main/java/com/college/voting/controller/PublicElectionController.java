package com.college.voting.controller;

import com.college.voting.entity.Election;
import com.college.voting.repository.ElectionParticipationRepository;
import com.college.voting.repository.StudentRepository;
import com.college.voting.service.ElectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
public class PublicElectionController {

    private final ElectionService electionService;
    private final StudentRepository studentRepository;
    private final ElectionParticipationRepository participationRepository;

    public PublicElectionController(ElectionService electionService,
                                    StudentRepository studentRepository,
                                    ElectionParticipationRepository participationRepository) {
        this.electionService = electionService;
        this.studentRepository = studentRepository;
        this.participationRepository = participationRepository;
    }

    @GetMapping("/election-status")
    public ResponseEntity<?> getPublicElectionStatus() {
        Optional<Election> activeElectionOpt = electionService.getActiveElection();
        Map<String, Object> response = new HashMap<>();

        if (activeElectionOpt.isPresent()) {
            Election election = activeElectionOpt.get();
            long totalStudents = studentRepository.count();
            long votesCast = participationRepository.countByElectionId(election.getId());
            long remainingStudents = Math.max(0, totalStudents - votesCast);

            response.put("active", true);
            response.put("title", election.getTitle());
            response.put("status", election.getStatus().toString());
            response.put("endTime", election.getEndTime());
            response.put("totalStudents", totalStudents);
            response.put("votesCast", votesCast);
            response.put("remainingStudents", remainingStudents);
        } else {
            response.put("active", false);
            response.put("title", "No Active Election");
            response.put("status", "INACTIVE");
            response.put("endTime", null);
            response.put("totalStudents", 0);
            response.put("votesCast", 0);
            response.put("remainingStudents", 0);
        }

        return ResponseEntity.ok(response);
    }
}
