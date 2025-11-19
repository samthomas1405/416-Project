package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "WaDemographicsPrecinct")
@CompoundIndex(name = "precinct_demo_unique", def = "{'countyCode':1,'precinctCode':1,'precinctPart':1,'ageGroup2024':1,'gender':1,'statusCode':1}", unique = true)
public class WaDemographicsPrecinctDoc {
    @Id
    private String id;  // stateAbbr|countyCode|precinctCode|precinctPart|ageGroup2024|gender|statusCode
    private String stateAbbr;
    private String countyCode;
    private String countyName;
    private String precinctCode;
    private String precinctPart;
    private String legislativeDistrict;
    private String congressionalDistrict;
    private String ageGroup2024;
    private String gender;
    private String statusCode;
    private Integer registeredVoters;
    private Date createdAt;
    private Date updatedAt;

    public WaDemographicsPrecinctDoc() {
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
    public String getPrecinctCode() { return precinctCode; }
    public void setPrecinctCode(String precinctCode) { this.precinctCode = precinctCode; }
    public String getPrecinctPart() { return precinctPart; }
    public void setPrecinctPart(String precinctPart) { this.precinctPart = precinctPart; }
    public String getLegislativeDistrict() { return legislativeDistrict; }
    public void setLegislativeDistrict(String legislativeDistrict) { this.legislativeDistrict = legislativeDistrict; }
    public String getCongressionalDistrict() { return congressionalDistrict; }
    public void setCongressionalDistrict(String congressionalDistrict) { this.congressionalDistrict = congressionalDistrict; }
    public String getAgeGroup2024() { return ageGroup2024; }
    public void setAgeGroup2024(String ageGroup2024) { this.ageGroup2024 = ageGroup2024; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public Integer getRegisteredVoters() { return registeredVoters; }
    public void setRegisteredVoters(Integer registeredVoters) { this.registeredVoters = registeredVoters; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



