package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "WaDemographicsCounty")
@CompoundIndex(name = "county_age_unique", def = "{'countyCode':1,'ageGroup2024':1}", unique = true)
public class WaDemographicsCountyDoc {
    @Id
    private String id;  // stateAbbr|countyCode|ageGroup2024
    private String stateAbbr;
    private String countyCode;
    private String countyName;
    private String ageGroup2024;
    private Integer registeredVoters;
    private Date createdAt;
    private Date updatedAt;

    public WaDemographicsCountyDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getCountyCode() { return countyCode; }
    public void setCountyCode(String countyCode) { this.countyCode = countyCode; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
    public String getAgeGroup2024() { return ageGroup2024; }
    public void setAgeGroup2024(String ageGroup2024) { this.ageGroup2024 = ageGroup2024; }
    public Integer getRegisteredVoters() { return registeredVoters; }
    public void setRegisteredVoters(Integer registeredVoters) { this.registeredVoters = registeredVoters; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



