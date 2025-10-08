package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "cvap")
@CompoundIndex(name = "region_year_unique", def = "{'regionId':1,'year':1}", unique = true)
public class Cvap {
    @Id
    private String id;         // "NY-36061|2023"
    private String stateId;
    private String regionId;
    private Integer year;
    private Integer total;
    private Map<String, Integer> byRaceEth;
    private Date createdAt;
    private Date updatedAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Map<String, Integer> getByRaceEth() { return byRaceEth; }
    public void setByRaceEth(Map<String, Integer> byRaceEth) { this.byRaceEth = byRaceEth; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
