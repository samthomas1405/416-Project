package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "WaRegistrationAge")
@CompoundIndex(name = "state_county_age_unique", def = "{'stateAbbr':1,'countyName':1,'ageGroup':1}", unique = true)
public class WaRegistrationAgeDoc {
    @Id
    private String id;  // stateAbbr|countyName|ageGroup
    private String stateAbbr;
    private String countyName;
    private String ageGroup;
    private Integer registeredVoters;
    private Date createdAt;
    private Date updatedAt;

    public WaRegistrationAgeDoc() {
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
    public Integer getRegisteredVoters() { return registeredVoters; }
    public void setRegisteredVoters(Integer registeredVoters) { this.registeredVoters = registeredVoters; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



