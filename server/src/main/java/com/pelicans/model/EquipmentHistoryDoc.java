package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "EquipmentHistory")
@CompoundIndex(name = "state_year_category_unique", def = "{'stateAbbr':1,'year':1,'equipmentCategory':1}", unique = true)
@CompoundIndex(name = "state_year_idx", def = "{'stateAbbr':1,'year':1}")
@CompoundIndex(name = "state_category_idx", def = "{'stateAbbr':1,'equipmentCategory':1}")
public class EquipmentHistoryDoc {
    @Id
    private String id;  // stateAbbr|year|equipmentCategory
    private String stateAbbr;
    private Integer year;
    private String equipmentCategory;
    private Integer deviceCount;
    private Date createdAt;
    private Date updatedAt;

    public EquipmentHistoryDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getEquipmentCategory() { return equipmentCategory; }
    public void setEquipmentCategory(String equipmentCategory) { this.equipmentCategory = equipmentCategory; }
    public Integer getDeviceCount() { return deviceCount; }
    public void setDeviceCount(Integer deviceCount) { this.deviceCount = deviceCount; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

