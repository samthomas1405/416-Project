package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "derived_metrics")
@CompoundIndex(name = "state_year_unique", def = "{'stateId':1,'year':1}", unique = true)
public class DerivedMetric {
    @Id
    private String id;            // "NY|2024"
    private String stateId;
    private Integer year;
    private Map<String, Object> regression; // e.g. qualityVsRejected.DEM.{a,b,c}
    private Map<String, Object> yoy;
    private Date createdAt;
    private Date updatedAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Map<String, Object> getRegression() { return regression; }
    public void setRegression(Map<String, Object> regression) { this.regression = regression; }
    public Map<String, Object> getYoy() { return yoy; }
    public void setYoy(Map<String, Object> yoy) { this.yoy = yoy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
