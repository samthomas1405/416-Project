package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "voter_registration_summary")
@CompoundIndex(name = "state_region_year_unique", def = "{'stateId':1,'regionId':1,'year':1}", unique = true)
public class VoterRegistrationSummary {
    @Id
    private String id;         // "NY|NY-36061|2024"
    private Integer year;
    private String stateId;
    private String regionId;
    private Integer registered;
    private Map<String, Integer> byParty; // DEM, REP, UNAFF, ...
    private Date createdAt;
    private Date updatedAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public Integer getRegistered() { return registered; }
    public void setRegistered(Integer registered) { this.registered = registered; }
    public Map<String, Integer> getByParty() { return byParty; }
    public void setByParty(Map<String, Integer> byParty) { this.byParty = byParty; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
