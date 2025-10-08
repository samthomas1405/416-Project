package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "policies")
public class Policy {
    public enum FelonyPolicy { none, auto_on_release, after_parole_probation, additional_action }

    @Id
    private String id;               // stateId
    @Indexed(unique = true)
    private String stateId;
    private FelonyPolicy felonyVoting;
    private String notes;
    private String asOf;             // ISO date string
    private Date createdAt;
    private Date updatedAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public FelonyPolicy getFelonyVoting() { return felonyVoting; }
    public void setFelonyVoting(FelonyPolicy felonyVoting) { this.felonyVoting = felonyVoting; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAsOf() { return asOf; }
    public void setAsOf(String asOf) { this.asOf = asOf; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
