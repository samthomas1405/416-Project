package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "FelonyPolicy")
public class FelonyPolicyDoc {
    @Id
    private String id;  // stateAbbr
    private String stateAbbr;
    private String stateFull;
    private Map<String, String> q51Fields;  // All Q51_* fields
    private Date createdAt;
    private Date updatedAt;

    public FelonyPolicyDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getStateFull() { return stateFull; }
    public void setStateFull(String stateFull) { this.stateFull = stateFull; }
    public Map<String, String> getQ51Fields() { return q51Fields; }
    public void setQ51Fields(Map<String, String> q51Fields) { this.q51Fields = q51Fields; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



