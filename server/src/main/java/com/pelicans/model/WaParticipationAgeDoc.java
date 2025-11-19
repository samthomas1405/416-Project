package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "WaParticipationAge")
@CompoundIndex(name = "state_county_age_year_election_unique", def = "{'stateAbbr':1,'countyName':1,'ageGroup':1,'year':1,'electionType':1}", unique = true)
public class WaParticipationAgeDoc {
    @Id
    private String id;  // stateAbbr|countyName|ageGroup|year|electionType
    private String stateAbbr;
    private String countyName;
    private String ageGroup;
    private Integer year;
    private String electionType;
    private Integer totalPopulation;
    private Integer totalVoters;
    private Double registeredPopulationShare;
    private Double voterTurnoutShare;
    private Date createdAt;
    private Date updatedAt;

    public WaParticipationAgeDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getElectionType() { return electionType; }
    public void setElectionType(String electionType) { this.electionType = electionType; }
    public Integer getTotalPopulation() { return totalPopulation; }
    public void setTotalPopulation(Integer totalPopulation) { this.totalPopulation = totalPopulation; }
    public Integer getTotalVoters() { return totalVoters; }
    public void setTotalVoters(Integer totalVoters) { this.totalVoters = totalVoters; }
    public Double getRegisteredPopulationShare() { return registeredPopulationShare; }
    public void setRegisteredPopulationShare(Double registeredPopulationShare) { this.registeredPopulationShare = registeredPopulationShare; }
    public Double getVoterTurnoutShare() { return voterTurnoutShare; }
    public void setVoterTurnoutShare(Double voterTurnoutShare) { this.voterTurnoutShare = voterTurnoutShare; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



