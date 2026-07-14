package com.college.voting.controller;

import com.college.voting.entity.Student;
import com.college.voting.entity.Vote;
import com.college.voting.repository.VoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/votes")
public class AdminVoteRecordController {

    private final VoteRepository voteRepository;

    public AdminVoteRecordController(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    public static class VoteRecordDTO {
        private String studentName;
        private String registerNo;
        private String department;
        private String year;
        private String candidatesVoted;
        private LocalDateTime votedAt;

        public VoteRecordDTO(String studentName, String registerNo, String department, String year, String candidatesVoted, LocalDateTime votedAt) {
            this.studentName = studentName;
            this.registerNo = registerNo;
            this.department = department;
            this.year = year;
            this.candidatesVoted = candidatesVoted;
            this.votedAt = votedAt;
        }

        public String getStudentName() { return studentName; }
        public String getRegisterNo() { return registerNo; }
        public String getDepartment() { return department; }
        public String getYear() { return year; }
        public String getCandidatesVoted() { return candidatesVoted; }
        public LocalDateTime getVotedAt() { return votedAt; }
    }

    @GetMapping("/records")
    public ResponseEntity<?> getVoteRecords(@RequestParam Long electionId) {
        List<Vote> votes = voteRepository.findVotesWithStudentAndCandidateByElectionId(electionId);

        Map<Student, List<Vote>> votesByStudent = votes.stream()
            .filter(v -> v.getStudent() != null)
            .collect(Collectors.groupingBy(Vote::getStudent));

        List<VoteRecordDTO> records = votesByStudent.entrySet().stream()
            .map(entry -> {
                Student student = entry.getKey();
                List<Vote> studentVotes = entry.getValue();

                String candidatesList = studentVotes.stream()
                    .map(v -> v.getCandidate().getName() + " (" + v.getPosition() + ")")
                    .collect(Collectors.joining(", "));

                LocalDateTime voteTime = studentVotes.stream()
                    .map(Vote::getVoteTime)
                    .min(Comparator.naturalOrder())
                    .orElse(LocalDateTime.now());

                String academicYear = student.getYear();
                if ("1".equals(academicYear)) {
                    academicYear = "1st Year";
                } else if ("2".equals(academicYear)) {
                    academicYear = "2nd Year";
                } else if ("3".equals(academicYear)) {
                    academicYear = "3rd Year";
                } else if (academicYear != null && !academicYear.toLowerCase().contains("year")) {
                    academicYear = academicYear + " Year";
                }

                return new VoteRecordDTO(
                    student.getName(),
                    student.getRegisterNo(),
                    student.getDepartment(),
                    academicYear,
                    candidatesList,
                    voteTime
                );
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(records);
    }
}
