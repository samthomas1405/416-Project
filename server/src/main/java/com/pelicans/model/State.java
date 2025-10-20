package com.pelicans.model;

// import org.springframework.data.mongodb.core.index.CompoundIndex;
// import org.springframework.data.mongodb.core.mapping.Document;

// @Document(collection = "state_summaries")
// @CompoundIndex(name = "state_year_unique", def = "{'stateId':1,'year':1}", unique = true)
public class State {
    private String stateFips;
    private String party;
    private String state;
    private String felony_rights;
    private Double pct_mail;
    private Double pct_dropbox;
    private Double turnout_pct;
    private Integer total_vap;
    private Integer registered;
    private Integer turnout_count;
    private Integer total_votes;
    private Integer early_in_person;
    private Integer early_by_mail;
    private Integer drop_box_returns;

    // getters/setters
    public String getStateFips() { return stateFips; }
    public void setStateFips(String stateFips) { this.stateFips = stateFips; }
    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getFelonyRights() { return felony_rights; }
    public void setFelonyRights(String felony_rights) { this.felony_rights = felony_rights; }
    public Double getPctMail() { return pct_mail; }
    public void setPctMail(Double pct_mail) { this.pct_mail = pct_mail; }
    public Double getPctDropbox() { return pct_dropbox; }
    public void setPctDropbox(Double pct_dropbox) { this.pct_dropbox = pct_dropbox; }
    public Double getTurnoutPct() { return turnout_pct; }
    public void setTurnoutPct(Double turnout_pct) { this.turnout_pct = turnout_pct; }
    public Integer getTotalVap() { return total_vap; }
    public void setTotalVap(Integer total_vap) { this.total_vap = total_vap; }
    public Integer getRegistered() { return registered; }
    public void setRegistered(Integer registered) { this.registered = registered; }
    public Integer getTurnoutCount() { return turnout_count; }
    public void setTurnoutCount(Integer turnout_count) { this.turnout_count = turnout_count; }
    public Integer getTotalVotes() { return total_votes; }
    public void setTotalVotes(Integer total_votes) { this.total_votes = total_votes; }
    public Integer getEarlyInPerson() { return early_in_person; }
    public void setEarlyInPerson(Integer early_in_person) { this.early_in_person = early_in_person; }
    public Integer getEarlyByMail() { return early_by_mail; }
    public void setEarlyByMail(Integer early_by_mail) { this.early_by_mail = early_by_mail; }
    public Integer getDropBoxReturns() { return drop_box_returns; }
    public void setDropBoxReturns(Integer drop_box_returns) { this.drop_box_returns = drop_box_returns; }
}
