package com.college.voting.service;

import com.college.voting.entity.Candidate;
import com.college.voting.entity.Election;
import com.college.voting.repository.CandidateRepository;
import com.college.voting.repository.ElectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public CandidateService(CandidateRepository candidateRepository, ElectionRepository electionRepository) {
        this.candidateRepository = candidateRepository;
        this.electionRepository = electionRepository;
    }

    public List<Candidate> getCandidatesByElection(Long electionId) {
        return candidateRepository.findByElectionId(electionId);
    }

    public List<Candidate> searchCandidates(Long electionId, String name) {
        if (name != null && !name.trim().isEmpty()) {
            return candidateRepository.findByElectionIdAndNameContainingIgnoreCase(electionId, name);
        }
        return getCandidatesByElection(electionId);
    }

    public Optional<Candidate> getCandidateById(Long id) {
        return candidateRepository.findById(id);
    }

    @Transactional
    public Candidate addCandidate(Long electionId, Candidate candidate, MultipartFile photoFile) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found."));

        if (candidateRepository.existsByElectionIdAndRegisterNo(electionId, candidate.getRegisterNo())) {
            throw new RuntimeException("Candidate with register number '" + candidate.getRegisterNo() + "' is already registered in this election.");
        }

        candidate.setElection(election);

        if (photoFile != null && !photoFile.isEmpty()) {
            String fileName = savePhoto(photoFile);
            candidate.setPhoto("/uploads/candidates/" + fileName);
        } else {
            candidate.setPhoto("/images/default-candidate.png");
        }

        return candidateRepository.save(candidate);
    }

    @Transactional
    public Candidate updateCandidate(Long id, Candidate details, MultipartFile photoFile) {
        Candidate candidate = candidateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Candidate not found."));

        if (!candidate.getRegisterNo().equalsIgnoreCase(details.getRegisterNo()) &&
            candidateRepository.existsByElectionIdAndRegisterNo(candidate.getElection().getId(), details.getRegisterNo())) {
            throw new RuntimeException("Register Number is already in use by another candidate.");
        }

        candidate.setName(details.getName());
        candidate.setRegisterNo(details.getRegisterNo());
        candidate.setDepartment(details.getDepartment());
        candidate.setYear(details.getYear());
        candidate.setPosition(details.getPosition());
        candidate.setManifesto(details.getManifesto());

        if (photoFile != null && !photoFile.isEmpty()) {
            String fileName = savePhoto(photoFile);
            candidate.setPhoto("/uploads/candidates/" + fileName);
        }

        return candidateRepository.save(candidate);
    }

    @Transactional
    public void deleteCandidate(Long id) {
        if (!candidateRepository.existsById(id)) {
            throw new RuntimeException("Candidate not found.");
        }
        candidateRepository.deleteById(id);
    }

    private String savePhoto(MultipartFile file) {
        try {
            String subDir = "candidates";
            Path dirPath = Paths.get(uploadDir, subDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String uniqueName = UUID.randomUUID().toString() + extension;
            Path filePath = dirPath.resolve(uniqueName);
            Files.copy(file.getInputStream(), filePath);

            return uniqueName;
        } catch (Exception e) {
            throw new RuntimeException("Could not save candidate photo: " + e.getMessage());
        }
    }
}
