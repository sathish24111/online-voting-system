package com.college.voting.service;

import com.college.voting.entity.*;
import com.college.voting.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final VoteRepository voteRepository;
    private final StudentRepository studentRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionParticipationRepository participationRepository;

    public ResultService(VoteRepository voteRepository,
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

    public static class CandidateResult {
        private Long candidateId;
        private String name;
        private String department;
        private String position;
        private String photo;
        private long voteCount;
        private double percentage;
        private String label = ""; // "WINNER", "RUNNER_UP", ""

        // Getters and Setters
        public Long getCandidateId() { return candidateId; }
        public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public String getPhoto() { return photo; }
        public void setPhoto(String photo) { this.photo = photo; }
        public long getVoteCount() { return voteCount; }
        public void setVoteCount(long voteCount) { this.voteCount = voteCount; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class ElectionResultSummary {
        private String electionTitle;
        private String electionStatus;
        private long totalStudents;
        private long votesCast; // Unique voters participated
        private double participationRate;
        private Map<String, List<CandidateResult>> resultsByPosition;

        // Getters and Setters
        public String getElectionTitle() { return electionTitle; }
        public void setElectionTitle(String electionTitle) { this.electionTitle = electionTitle; }
        public String getElectionStatus() { return electionStatus; }
        public void setElectionStatus(String electionStatus) { this.electionStatus = electionStatus; }
        public long getTotalStudents() { return totalStudents; }
        public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }
        public long getVotesCast() { return votesCast; }
        public void setVotesCast(long votesCast) { this.votesCast = votesCast; }
        public double getParticipationRate() { return participationRate; }
        public void setParticipationRate(double participationRate) { this.participationRate = participationRate; }
        public Map<String, List<CandidateResult>> getResultsByPosition() { return resultsByPosition; }
        public void setResultsByPosition(Map<String, List<CandidateResult>> resultsByPosition) { this.resultsByPosition = resultsByPosition; }
    }

    public ElectionResultSummary getElectionResults(Long electionId, boolean isAdminRequest) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found."));

        // Enforce result invisibility if election is active for students
        if (!isAdminRequest && !"ENDED".equalsIgnoreCase(election.getStatus())) {
            throw new RuntimeException("Election results are hidden while the voting is active. Results will be published after the election ends.");
        }

        ElectionResultSummary summary = new ElectionResultSummary();
        summary.setElectionTitle(election.getTitle());
        summary.setElectionStatus(election.getStatus());

        // Turnout calculations
        long totalEnabledStudents = studentRepository.count(); // Count of all registered students
        long votesCastCount = participationRepository.countByElectionId(electionId);
        summary.setTotalStudents(totalEnabledStudents);
        summary.setVotesCast(votesCastCount);
        double participationRate = totalEnabledStudents > 0 ? ((double) votesCastCount / totalEnabledStudents) * 100 : 0;
        summary.setParticipationRate(Math.round(participationRate * 100.0) / 100.0);

        // Fetch candidates and votes map
        List<Candidate> candidates = candidateRepository.findByElectionId(electionId);
        List<Object[]> dbVotes = voteRepository.countVotesGroupByCandidate(electionId);
        Map<Long, Long> voteCountMap = dbVotes.stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
            ));

        // Group candidates by position
        Map<String, List<Candidate>> candidatesByPosition = candidates.stream()
            .collect(Collectors.groupingBy(Candidate::getPosition));

        Map<String, List<CandidateResult>> resultsByPosition = new HashMap<>();

        for (Map.Entry<String, List<Candidate>> entry : candidatesByPosition.entrySet()) {
            String position = entry.getKey();
            List<Candidate> positionCandidates = entry.getValue();

            // Calculate total votes cast for this position
            long totalVotesForPosition = positionCandidates.stream()
                .mapToLong(c -> voteCountMap.getOrDefault(c.getId(), 0L))
                .sum();

            List<CandidateResult> candResults = new ArrayList<>();
            for (Candidate cand : positionCandidates) {
                CandidateResult cr = new CandidateResult();
                cr.setCandidateId(cand.getId());
                cr.setName(cand.getName());
                cr.setDepartment(cand.getDepartment());
                cr.setPosition(cand.getPosition());
                cr.setPhoto(cand.getPhoto());
                
                long count = voteCountMap.getOrDefault(cand.getId(), 0L);
                cr.setVoteCount(count);
                
                double pct = totalVotesForPosition > 0 ? ((double) count / totalVotesForPosition) * 100 : 0;
                cr.setPercentage(Math.round(pct * 100.0) / 100.0);
                
                candResults.add(cr);
            }

            // Sort candidates by vote count descending
            candResults.sort(Comparator.comparingLong(CandidateResult::getVoteCount).reversed());

            // Label Winner and Runner-up with tie detection
            if (!candResults.isEmpty() && candResults.get(0).getVoteCount() > 0) {
                long maxVotes = candResults.get(0).getVoteCount();
                List<CandidateResult> maxVoteCandidates = candResults.stream()
                    .filter(cr -> cr.getVoteCount() == maxVotes)
                    .toList();

                if (maxVoteCandidates.size() > 1) {
                    // Tie for the first place
                    for (CandidateResult cr : maxVoteCandidates) {
                        cr.setLabel("TIE");
                    }
                } else {
                    // Single Winner
                    candResults.get(0).setLabel("WINNER");
                    if (candResults.size() > 1 && candResults.get(1).getVoteCount() > 0) {
                        long runnerUpVotes = candResults.get(1).getVoteCount();
                        for (CandidateResult cr : candResults) {
                            if (cr.getVoteCount() == runnerUpVotes) {
                                cr.setLabel("RUNNER_UP");
                            }
                        }
                    }
                }
            }

            resultsByPosition.put(position, candResults);
        }

        summary.setResultsByPosition(resultsByPosition);
        return summary;
    }

    public byte[] exportToExcel(Long electionId) throws Exception {
        ElectionResultSummary results = getElectionResults(electionId, true);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Results");

        // Title row
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("ELECTION RESULTS: " + results.getElectionTitle());
        
        Row metaRow = sheet.createRow(1);
        metaRow.createCell(0).setCellValue("Total Voters: " + results.getTotalStudents() + 
                                           " | Votes Cast: " + results.getVotesCast() + 
                                           " | Turnout: " + results.getParticipationRate() + "%");

        int rowIndex = 3;
        for (Map.Entry<String, List<CandidateResult>> entry : results.getResultsByPosition().entrySet()) {
            Row posHeader = sheet.createRow(rowIndex++);
            Cell posCell = posHeader.createCell(0);
            posCell.setCellValue("POSITION: " + entry.getKey().toUpperCase());
            
            // Header Row
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("Candidate Name");
            headerRow.createCell(1).setCellValue("Department");
            headerRow.createCell(2).setCellValue("Votes");
            headerRow.createCell(3).setCellValue("Percentage");
            headerRow.createCell(4).setCellValue("Outcome");

            for (CandidateResult cr : entry.getValue()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(cr.getName());
                row.createCell(1).setCellValue(cr.getDepartment());
                row.createCell(2).setCellValue(cr.getVoteCount());
                row.createCell(3).setCellValue(cr.getPercentage() + "%");
                row.createCell(4).setCellValue(cr.getLabel());
            }
            rowIndex++; // Add spacing
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    public byte[] exportToCsv(Long electionId) {
        ElectionResultSummary results = getElectionResults(electionId, true);
        StringBuilder sb = new StringBuilder();
        
        sb.append("Election: ").append(results.getElectionTitle()).append("\n");
        sb.append("Total Registered,Votes Cast,Participation Rate\n");
        sb.append(results.getTotalStudents()).append(",")
          .append(results.getVotesCast()).append(",")
          .append(results.getParticipationRate()).append("%\n\n");

        sb.append("Position,Candidate Name,Department,Votes,Percentage,Outcome\n");
        for (Map.Entry<String, List<CandidateResult>> entry : results.getResultsByPosition().entrySet()) {
            String position = entry.getKey();
            for (CandidateResult cr : entry.getValue()) {
                sb.append("\"").append(position).append("\",")
                  .append("\"").append(cr.getName()).append("\",")
                  .append("\"").append(cr.getDepartment()).append("\",")
                  .append(cr.getVoteCount()).append(",")
                  .append(cr.getPercentage()).append("%,")
                  .append(cr.getLabel()).append("\n");
            }
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportToPdf(Long electionId) throws Exception {
        ElectionResultSummary results = getElectionResults(electionId, true);
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        
        document.open();
        
        // Font setup
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font subTitleFont = new Font(Font.HELVETICA, 12, Font.ITALIC);
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        
        // Title
        document.add(new Paragraph("COLLEGE UNION ELECTION RESULTS", titleFont));
        document.add(new Paragraph("Election: " + results.getElectionTitle(), subTitleFont));
        document.add(new Paragraph("Date Generated: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        document.add(new Paragraph("------------------------------------------------------------------------------------------\n"));

        // Summary metrics
        document.add(new Paragraph(String.format("Total Voters: %d   |   Ballots Submitted: %d   |   Voter Turnout: %.2f%%\n\n", 
            results.getTotalStudents(), results.getVotesCast(), results.getParticipationRate())));

        for (Map.Entry<String, List<CandidateResult>> entry : results.getResultsByPosition().entrySet()) {
            document.add(new Paragraph("Position: " + entry.getKey(), sectionFont));
            document.add(new Paragraph(" "));
            
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2.5f, 1.5f, 1.5f, 1.5f});

            // Table Header
            table.addCell(new PdfPCell(new Phrase("Candidate Name", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Department", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Votes", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Percentage", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Outcome", headerFont)));

            for (CandidateResult cr : entry.getValue()) {
                table.addCell(cr.getName());
                table.addCell(cr.getDepartment());
                table.addCell(String.valueOf(cr.getVoteCount()));
                table.addCell(cr.getPercentage() + "%");
                table.addCell(cr.getLabel());
            }

            document.add(table);
            document.add(new Paragraph("\n"));
        }

        document.close();
        return out.toByteArray();
    }
}
