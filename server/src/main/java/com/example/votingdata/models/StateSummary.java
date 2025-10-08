package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "state_summaries")
@CompoundIndex(name = "state_year_unique", def = "{'stateId':1,'year':1}", unique = true)
public class StateSummary {
    @Id
    private String id;          // "NY|2024"
    private Integer year;
    private String stateId;
    private Double turnoutPct;
    private Double regRatePct;
    private Double mailPct;
    private Double dropBoxPct;
    private Policy.FelonyPolicy felonyPolicy;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public Double getTurnoutPct() { return turnoutPct; }
    public void setTurnoutPct(Double turnoutPct) { this.turnoutPct = turnoutPct; }
    public Double getRegRatePct() { return regRatePct; }
    public void setRegRatePct(Double regRatePct) { this.regRatePct = regRatePct; }
    public Double getMailPct() { return mailPct; }
    public void setMailPct(Double mailPct) { this.mailPct = mailPct; }
    public Double getDropBoxPct() { return dropBoxPct; }
    public void setDropBoxPct(Double dropBoxPct) { this.dropBoxPct = dropBoxPct; }
    public Policy.FelonyPolicy getFelonyPolicy() { return felonyPolicy; }
    public void setFelonyPolicy(Policy.FelonyPolicy felonyPolicy) { this.felonyPolicy = felonyPolicy; }
}
