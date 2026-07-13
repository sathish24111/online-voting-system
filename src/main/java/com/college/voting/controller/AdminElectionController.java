package com.college.voting.controller;

import com.college.voting.entity.Election;
import com.college.voting.service.AuditLogService;
import com.college.voting.service.ElectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/elections")
public class AdminElectionController {

    private final ElectionService electionService;
    private final AuditLogService auditLogService;

    public AdminElectionController(ElectionService electionService, AuditLogService auditLogService) {
        this.electionService = electionService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<Election>> getAllElections() {
        return ResponseEntity.ok(electionService.getAllElections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getElectionById(@PathVariable Long id) {
        return electionService.getElectionById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<?> createElection(@RequestBody Election election, HttpServletRequest request, Principal principal) {
        try {
            Election created = electionService.createElection(election);
            auditLogService.log(principal.getName(), "Created Election: " + created.getTitle(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateElection(@PathVariable Long id, @RequestBody Election election, HttpServletRequest request, Principal principal) {
        try {
            Election updated = electionService.updateElection(id, election);
            auditLogService.log(principal.getName(), "Updated Election: " + updated.getTitle(), request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request, Principal principal) {
        try {
            String status = body.get("status");
            Election updated = electionService.changeStatus(id, status);
            auditLogService.log(principal.getName(), "Changed status of Election '" + updated.getTitle() + "' to " + updated.getStatus(), request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteElection(@PathVariable Long id, HttpServletRequest request, Principal principal) {
        try {
            electionService.deleteElection(id);
            auditLogService.log(principal.getName(), "Deleted Election ID: " + id, request);
            return ResponseEntity.ok(Map.of("message", "Election deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
