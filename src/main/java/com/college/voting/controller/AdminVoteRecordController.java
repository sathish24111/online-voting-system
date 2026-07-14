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
                if (academicYear != null) {
                    String y = academicYear.trim().toUpperCase();
                    if (y.startsWith("III") || y.startsWith("3")) {
                        academicYear = "3rd Year";
                    } else if (y.startsWith("II") || y.startsWith("2")) {
                        academicYear = "2nd Year";
                    } else if (y.startsWith("I") || y.startsWith("1")) {
                        academicYear = "1st Year";
                    }
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

    @GetMapping("/records/export/excel")
    public ResponseEntity<?> exportVoteRecordsExcel(@RequestParam Long electionId) {
        try {
            ResponseEntity<?> response = getVoteRecords(electionId);
            @SuppressWarnings("unchecked")
            List<VoteRecordDTO> records = (List<VoteRecordDTO>) response.getBody();

            if (records == null) {
                records = new ArrayList<>();
            }

            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Vote Records");

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Title Row
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DETAILED VOTE AUDIT RECORDS");
            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Header Row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(2);
            String[] columns = {"S.No", "Student Name", "Roll Number", "Department", "Academic Year", "Candidate Voted For", "Vote Cast Time"};
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 3;
            int sNo = 1;
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            records.sort((a, b) -> {
                int yearComp = a.getYear().compareTo(b.getYear());
                if (yearComp != 0) return yearComp;
                return b.getVotedAt().compareTo(a.getVotedAt());
            });

            for (VoteRecordDTO r : records) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sNo++);
                row.createCell(1).setCellValue(r.getStudentName());
                row.createCell(2).setCellValue(r.getRegisterNo());
                row.createCell(3).setCellValue(r.getDepartment());
                row.createCell(4).setCellValue(r.getYear());
                row.createCell(5).setCellValue(r.getCandidatesVoted());
                row.createCell(6).setCellValue(r.getVotedAt().format(formatter));
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            byte[] bytes = out.toByteArray();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentDispositionFormData("attachment", "detailed_vote_records_election_" + electionId + ".xlsx");
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            return new org.springframework.http.ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate Excel report: " + e.getMessage()));
        }
    }
}
