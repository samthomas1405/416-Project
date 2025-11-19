package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "wa_voter")
public class WaVoterDoc {
    @Id
    private String id;  // stateVoterId
    @Indexed(unique = true)
    private String stateVoterId;
    private Integer birthyear;
    private Integer age2024;
    private String ageGroup2024;
    private String gender;
    private String countyCode;
    private String countyName;
    private String precinctCode;
    private String precinctPart;
    private String legislativeDistrict;
    private String congressionalDistrict;
    private String registrationDate;
    private String lastVoted;
    private String statusCode;
    private Date createdAt;
    private Date updatedAt;

    public WaVoterDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateVoterId() { return stateVoterId; }
    public void setStateVoterId(String stateVoterId) { this.stateVoterId = stateVoterId; }
    public Integer getBirthyear() { return birthyear; }
    public void setBirthyear(Integer birthyear) { this.birthyear = birthyear; }
    public Integer getAge2024() { return age2024; }
    public void setAge2024(Integer age2024) { this.age2024 = age2024; }
    public String getAgeGroup2024() { return ageGroup2024; }
    public void setAgeGroup2024(String ageGroup2024) { this.ageGroup2024 = ageGroup2024; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
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
    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }
    public String getLastVoted() { return lastVoted; }
    public void setLastVoted(String lastVoted) { this.lastVoted = lastVoted; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



