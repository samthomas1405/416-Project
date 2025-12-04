package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "WaVotersPartyAggregated")
@CompoundIndex(name = "wa_party_aggregated_unique", def = "{'stateFips':1,'countyCode':1,'countyMajorityParty':1,'ageGroup2024':1,'gender':1,'statusCode':1}", unique = true)
public class WaVotersPartyAggregatedDoc {
    @Id
    private String id;  // stateFips|countyCode|countyMajorityParty|ageGroup2024|gender|statusCode
    private Integer stateFips;
    private String countyCode;
    private String countyName;
    private String countyMajorityParty;  // REP or DEM
    private String ageGroup2024;
    private String gender;
    private String statusCode;
    private Integer voterCount;
    private Date createdAt;
    private Date updatedAt;

    public WaVotersPartyAggregatedDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getStateFips() { return stateFips; }
    public void setStateFips(Integer stateFips) { this.stateFips = stateFips; }
    public String getCountyCode() { return countyCode; }
    public void setCountyCode(String countyCode) { this.countyCode = countyCode; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
    public String getCountyMajorityParty() { return countyMajorityParty; }
    public void setCountyMajorityParty(String countyMajorityParty) { this.countyMajorityParty = countyMajorityParty; }
    public String getAgeGroup2024() { return ageGroup2024; }
    public void setAgeGroup2024(String ageGroup2024) { this.ageGroup2024 = ageGroup2024; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public Integer getVoterCount() { return voterCount; }
    public void setVoterCount(Integer voterCount) { this.voterCount = voterCount; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

