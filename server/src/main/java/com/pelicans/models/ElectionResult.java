package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "election_results")
@CompoundIndex(name = "yr_state_region_unique", def = "{'stateId':1,'regionId':1,'year':1}", unique = true)
// @CompoundIndex(name = "year_state_idx", def = "{'year':1,'stateId':1}")
public class ElectionResult {
    public static class Presidential {
        private Integer rep;
        private Integer dem;
        private Integer other;
        public Integer getRep() { return rep; }
        public void setRep(Integer rep) { this.rep = rep; }
        public Integer getDem() { return dem; }
        public void setDem(Integer dem) { this.dem = dem; }
        public Integer getOther() { return other; }
        public void setOther(Integer other) { this.other = other; }
    }
    public static class DropBoxVotes {
        private Integer C3a;
        public Integer getC3a() { return C3a; }
        public void setC3a(Integer c3a) { C3a = c3a; }
    }

    @Id
    private String id;         // "2024|NY|NY-36061"
    private Integer year;
    private String stateId;
    private String regionId;
    private Presidential presidential;
    private DropBoxVotes dropBoxVotes;
    private Date createdAt;
    private Date updatedAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public Presidential getPresidential() { return presidential; }
    public void setPresidential(Presidential presidential) { this.presidential = presidential; }
    public DropBoxVotes getDropBoxVotes() { return dropBoxVotes; }
    public void setDropBoxVotes(DropBoxVotes dropBoxVotes) { this.dropBoxVotes = dropBoxVotes; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
