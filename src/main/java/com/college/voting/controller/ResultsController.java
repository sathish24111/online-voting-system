package com.college.voting.controller;

import com.college.voting.service.AuditLogService;
import com.college.voting.service.ResultService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
public class ResultsController {

    private final ResultService resultService;
    private final AuditLogService auditLogService;

    public ResultsController(ResultService resultService, AuditLogService auditLogService) {
        this.resultService = resultService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/{electionId}")
    public ResponseEntity<?> getResults(@PathVariable Long electionId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            return ResponseEntity.ok(resultService.getElectionResults(electionId, isAdmin));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{electionId}/export/excel")
    public ResponseEntity<?> exportExcel(@PathVariable Long electionId, HttpServletRequest request, Principal principal) {
        try {
            byte[] bytes = resultService.exportToExcel(electionId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "results_election_" + electionId + ".xlsx");
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            
            auditLogService.log(principal.getName(), "Exported results to Excel for Election ID: " + electionId, request);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{electionId}/export/csv")
    public ResponseEntity<?> exportCsv(@PathVariable Long electionId, HttpServletRequest request, Principal principal) {
        try {
            byte[] bytes = resultService.exportToCsv(electionId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "results_election_" + electionId + ".csv");
            headers.setContentType(MediaType.TEXT_PLAIN);

            auditLogService.log(principal.getName(), "Exported results to CSV for Election ID: " + electionId, request);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{electionId}/export/pdf")
    public ResponseEntity<?> exportPdf(@PathVariable Long electionId, HttpServletRequest request, Principal principal) {
        try {
            byte[] bytes = resultService.exportToPdf(electionId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "results_election_" + electionId + ".pdf");
            headers.setContentType(MediaType.APPLICATION_PDF);

            auditLogService.log(principal.getName(), "Exported results to PDF for Election ID: " + electionId, request);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
