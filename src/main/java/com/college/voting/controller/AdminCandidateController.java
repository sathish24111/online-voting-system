package com.college.voting.controller;

import com.college.voting.entity.Candidate;
import com.college.voting.service.AuditLogService;
import com.college.voting.service.CandidateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/candidates")
public class AdminCandidateController {

    private final CandidateService candidateService;
    private final AuditLogService auditLogService;

    public AdminCandidateController(CandidateService candidateService, AuditLogService auditLogService) {
        this.candidateService = candidateService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<Candidate>> getCandidates(
            @RequestParam Long electionId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(candidateService.searchCandidates(electionId, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCandidate(@PathVariable Long id) {
        return candidateService.getCandidateById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<?> addCandidate(
            @RequestParam Long electionId,
            @RequestParam String name,
            @RequestParam String registerNo,
            @RequestParam String department,
            @RequestParam String year,
            @RequestParam String position,
            @RequestParam(required = false) String manifesto,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile,
            HttpServletRequest request,
            Principal principal) {
        try {
            Candidate candidate = new Candidate();
            candidate.setName(name);
            candidate.setRegisterNo(registerNo);
            candidate.setDepartment(department);
            candidate.setYear(year);
            candidate.setPosition(position);
            candidate.setManifesto(manifesto);

            Candidate saved = candidateService.addCandidate(electionId, candidate, photoFile);
            auditLogService.log(principal.getName(), "Added Candidate: " + saved.getName() + " for position " + saved.getPosition(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCandidate(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String registerNo,
            @RequestParam String department,
            @RequestParam String year,
            @RequestParam String position,
            @RequestParam(required = false) String manifesto,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile,
            HttpServletRequest request,
            Principal principal) {
        try {
            Candidate candidate = new Candidate();
            candidate.setName(name);
            candidate.setRegisterNo(registerNo);
            candidate.setDepartment(department);
            candidate.setYear(year);
            candidate.setPosition(position);
            candidate.setManifesto(manifesto);

            Candidate updated = candidateService.updateCandidate(id, candidate, photoFile);
            auditLogService.log(principal.getName(), "Updated Candidate ID: " + id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCandidate(@PathVariable Long id, HttpServletRequest request, Principal principal) {
        try {
            candidateService.deleteCandidate(id);
            auditLogService.log(principal.getName(), "Deleted Candidate ID: " + id, request);
            return ResponseEntity.ok(Map.of("message", "Candidate deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
