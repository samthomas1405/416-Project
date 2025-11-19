package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "EquipmentJurisdiction")
@CompoundIndex(name = "state_fips_unique", def = "{'stateAbbr':1,'fipsCode':1}", unique = true)
public class EquipmentJurisdictionDoc {
    @Id
    private String id;  // stateAbbr|fipsCode
    private String stateAbbr;
    private String fipsCode;
    private String stateName;
    private String jurisdiction;
    private Double avgQualityScore;
    private Date createdAt;
    private Date updatedAt;

    public EquipmentJurisdictionDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getFipsCode() { return fipsCode; }
    public void setFipsCode(String fipsCode) { this.fipsCode = fipsCode; }
    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    public Double getAvgQualityScore() { return avgQualityScore; }
    public void setAvgQualityScore(Double avgQualityScore) { this.avgQualityScore = avgQualityScore; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



