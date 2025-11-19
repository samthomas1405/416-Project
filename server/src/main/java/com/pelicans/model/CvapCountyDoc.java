package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "CvapCounty")
@CompoundIndex(name = "state_fips_category_unique", def = "{'stateAbbr':1,'fips5':1,'cvapCategoryCode':1}", unique = true)
@CompoundIndex(name = "state_fips_idx", def = "{'stateAbbr':1,'fips5':1}")
public class CvapCountyDoc {
    @Id
    private String id;  // stateAbbr|fips5|cvapCategoryCode
    private String stateAbbr;
    private String stateFips;
    private String stateName;
    private String fips5;
    private String countyName;
    private String geoid;
    private String cvapCategoryCode;
    private String cvapCategory;
    private Integer totalPopulationEst;
    private Integer adultPopulationEst;
    private Integer citizenPopulationEst;
    private Integer cvapEstimate;
    private Integer totalPopulationMoe;
    private Integer adultPopulationMoe;
    private Integer citizenPopulationMoe;
    private Integer cvapMoe;
    private Date createdAt;
    private Date updatedAt;

    public CvapCountyDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getStateFips() { return stateFips; }
    public void setStateFips(String stateFips) { this.stateFips = stateFips; }
    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }
    public String getFips5() { return fips5; }
    public void setFips5(String fips5) { this.fips5 = fips5; }
    public String getCountyName() { return countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }
    public String getGeoid() { return geoid; }
    public void setGeoid(String geoid) { this.geoid = geoid; }
    public String getCvapCategoryCode() { return cvapCategoryCode; }
    public void setCvapCategoryCode(String cvapCategoryCode) { this.cvapCategoryCode = cvapCategoryCode; }
    public String getCvapCategory() { return cvapCategory; }
    public void setCvapCategory(String cvapCategory) { this.cvapCategory = cvapCategory; }
    public Integer getTotalPopulationEst() { return totalPopulationEst; }
    public void setTotalPopulationEst(Integer totalPopulationEst) { this.totalPopulationEst = totalPopulationEst; }
    public Integer getAdultPopulationEst() { return adultPopulationEst; }
    public void setAdultPopulationEst(Integer adultPopulationEst) { this.adultPopulationEst = adultPopulationEst; }
    public Integer getCitizenPopulationEst() { return citizenPopulationEst; }
    public void setCitizenPopulationEst(Integer citizenPopulationEst) { this.citizenPopulationEst = citizenPopulationEst; }
    public Integer getCvapEstimate() { return cvapEstimate; }
    public void setCvapEstimate(Integer cvapEstimate) { this.cvapEstimate = cvapEstimate; }
    public Integer getTotalPopulationMoe() { return totalPopulationMoe; }
    public void setTotalPopulationMoe(Integer totalPopulationMoe) { this.totalPopulationMoe = totalPopulationMoe; }
    public Integer getAdultPopulationMoe() { return adultPopulationMoe; }
    public void setAdultPopulationMoe(Integer adultPopulationMoe) { this.adultPopulationMoe = adultPopulationMoe; }
    public Integer getCitizenPopulationMoe() { return citizenPopulationMoe; }
    public void setCitizenPopulationMoe(Integer citizenPopulationMoe) { this.citizenPopulationMoe = citizenPopulationMoe; }
    public Integer getCvapMoe() { return cvapMoe; }
    public void setCvapMoe(Integer cvapMoe) { this.cvapMoe = cvapMoe; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



