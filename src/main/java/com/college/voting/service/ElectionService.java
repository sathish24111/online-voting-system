package com.college.voting.service;

import com.college.voting.entity.Election;
import com.college.voting.repository.ElectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ElectionService {

    private final ElectionRepository electionRepository;

    public ElectionService(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }

    public Optional<Election> getElectionById(Long id) {
        return electionRepository.findById(id);
    }

    @Transactional
    public Election createElection(Election election) {
        if (election.getStartTime().isAfter(election.getEndTime())) {
            throw new RuntimeException("Start time must be before end time.");
        }
        election.setStatus("NOT_STARTED");
        return electionRepository.save(election);
    }

    @Transactional
    public Election updateElection(Long id, Election details) {
        Election election = electionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Election not found."));

        if (details.getStartTime().isAfter(details.getEndTime())) {
            throw new RuntimeException("Start time must be before end time.");
        }

        election.setTitle(details.getTitle());
        election.setStartTime(details.getStartTime());
        election.setEndTime(details.getEndTime());
        
        return electionRepository.save(election);
    }

    @Transactional
    public Election changeStatus(Long id, String status) {
        Election election = electionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Election not found."));

        String upperStatus = status.toUpperCase();
        if (!upperStatus.equals("NOT_STARTED") && !upperStatus.equals("RUNNING") && 
            !upperStatus.equals("PAUSED") && !upperStatus.equals("ENDED")) {
            throw new RuntimeException("Invalid election status: " + status);
        }

        // If status is changed to RUNNING, verify active period or update start/end times if needed
        election.setStatus(upperStatus);
        return electionRepository.save(election);
    }

    @Transactional
    public void deleteElection(Long id) {
        if (!electionRepository.existsById(id)) {
            throw new RuntimeException("Election not found.");
        }
        electionRepository.deleteById(id);
    }

    public Optional<Election> getActiveElection() {
        // Return the first running election
        List<Election> running = electionRepository.findByStatus("RUNNING");
        if (!running.isEmpty()) {
            Election e = running.get(0);
            // Check if current time is within election period.
            // If it has exceeded end time, we should automatically update the status to ENDED!
            // This is a great, robust, self-correcting design!
            if (LocalDateTime.now().isAfter(e.getEndTime())) {
                e.setStatus("ENDED");
                electionRepository.save(e);
                return Optional.empty();
            }
            return Optional.of(e);
        }
        return Optional.empty();
    }
}
