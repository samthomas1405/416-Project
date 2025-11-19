package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "wa_voter_history")
public class WaVoterHistoryDoc {
    @Id
    private String id;  // voterHistoryId
    private String stateAbbr;
    private String voterHistoryId;
    @Indexed
    private String stateVoterId;
    private String countyCode;
    private String countyCodeVoting;
    private Date electionDate;
    private String electionDateStr;
    private Integer electionYear;
    private Date createdAt;
    private Date updatedAt;

    public WaVoterHistoryDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getVoterHistoryId() { return voterHistoryId; }
    public void setVoterHistoryId(String voterHistoryId) { this.voterHistoryId = voterHistoryId; }
    public String getStateVoterId() { return stateVoterId; }
    public void setStateVoterId(String stateVoterId) { this.stateVoterId = stateVoterId; }
    public String getCountyCode() { return countyCode; }
    public void setCountyCode(String countyCode) { this.countyCode = countyCode; }
    public String getCountyCodeVoting() { return countyCodeVoting; }
    public void setCountyCodeVoting(String countyCodeVoting) { this.countyCodeVoting = countyCodeVoting; }
    public Date getElectionDate() { return electionDate; }
    public void setElectionDate(Date electionDate) { this.electionDate = electionDate; }
    public String getElectionDateStr() { return electionDateStr; }
    public void setElectionDateStr(String electionDateStr) { this.electionDateStr = electionDateStr; }
    public Integer getElectionYear() { return electionYear; }
    public void setElectionYear(Integer electionYear) { this.electionYear = electionYear; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



