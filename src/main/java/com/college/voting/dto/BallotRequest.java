package com.college.voting.dto;

import java.util.List;

public class BallotRequest {
    private Long electionId;
    private List<Long> candidateIds;

    public BallotRequest() {}

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public List<Long> getCandidateIds() {
        return candidateIds;
    }

    public void setCandidateIds(List<Long> candidateIds) {
        this.candidateIds = candidateIds;
    }
}
