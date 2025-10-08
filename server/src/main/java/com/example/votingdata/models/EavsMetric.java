package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "eavs_metrics")
@CompoundIndex(name = "yr_state_region_unique", def = "{'stateId':1,'regionId':1,'year':1}", unique = true)
@CompoundIndex(name = "year_state_idx", def = "{'year':1,'stateId':1}")
public class EavsMetric {
    @Id
    private String id;           // "2024|NY|NY-36061"
    private int year;
    private String stateId;
    private String regionId;

    private Categories categories;
    private Map<String, Object> derived;     // { missingnessScore, turnoutPct }
    private Map<String, Object> provenance;  // { source, version, ingestedAt, fileHash }
    private Date createdAt;
    private Date updatedAt;

    // ---- nested ----
    public static class Categories {
        private Map<String, Object> provisional;
        private Map<String, Object> activeVoters;
        private Map<String, Object> pollbookDeletions;
        private Map<String, Object> mailRejections;
        private EarlyVoting earlyVoting;
        private Tot provisionalRejected; // { total }
        private Tot uocavaRejected;      // { total }
        public Map<String, Object> getProvisional() { return provisional; }
        public void setProvisional(Map<String, Object> provisional) { this.provisional = provisional; }
        public Map<String, Object> getActiveVoters() { return activeVoters; }
        public void setActiveVoters(Map<String, Object> activeVoters) { this.activeVoters = activeVoters; }
        public Map<String, Object> getPollbookDeletions() { return pollbookDeletions; }
        public void setPollbookDeletions(Map<String, Object> pollbookDeletions) { this.pollbookDeletions = pollbookDeletions; }
        public Map<String, Object> getMailRejections() { return mailRejections; }
        public void setMailRejections(Map<String, Object> mailRejections) { this.mailRejections = mailRejections; }
        public EarlyVoting getEarlyVoting() { return earlyVoting; }
        public void setEarlyVoting(EarlyVoting earlyVoting) { this.earlyVoting = earlyVoting; }
        public Tot getProvisionalRejected() { return provisionalRejected; }
        public void setProvisionalRejected(Tot provisionalRejected) { this.provisionalRejected = provisionalRejected; }
        public Tot getUocavaRejected() { return uocavaRejected; }
        public void setUocavaRejected(Tot uocavaRejected) { this.uocavaRejected = uocavaRejected; }
    }
    public static class EarlyVoting {
        private Double total;
        private Double inPerson;
        private Double mail;
        private Map<String, Object> raw;
        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }
        public Double getInPerson() { return inPerson; }
        public void setInPerson(Double inPerson) { this.inPerson = inPerson; }
        public Double getMail() { return mail; }
        public void setMail(Double mail) { this.mail = mail; }
        public Map<String, Object> getRaw() { return raw; }
        public void setRaw(Map<String, Object> raw) { this.raw = raw; }
    }
    public static class Tot {
        private Double total;
        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }
    }

    // ---- getters/setters ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public Categories getCategories() { return categories; }
    public void setCategories(Categories categories) { this.categories = categories; }
    public Map<String, Object> getDerived() { return derived; }
    public void setDerived(Map<String, Object> derived) { this.derived = derived; }
    public Map<String, Object> getProvenance() { return provenance; }
    public void setProvenance(Map<String, Object> provenance) { this.provenance = provenance; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
