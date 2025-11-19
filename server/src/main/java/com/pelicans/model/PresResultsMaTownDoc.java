package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "PresResultsMaTown")
@CompoundIndex(name = "state_town_unique", def = "{'stateAbbr':1,'townName':1}", unique = true)
public class PresResultsMaTownDoc {
    @Id
    private String id;  // stateAbbr|townName
    private String stateAbbr;
    private String townName;
    private Integer votesDem2024Pres;
    private Integer votesRep2024Pres;
    private Integer votesOther2024Pres;
    private Integer totalVotes2024Pres;
    private Double demShare2024Pres;
    private Double repShare2024Pres;
    private Date createdAt;
    private Date updatedAt;

    public PresResultsMaTownDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getTownName() { return townName; }
    public void setTownName(String townName) { this.townName = townName; }
    public Integer getVotesDem2024Pres() { return votesDem2024Pres; }
    public void setVotesDem2024Pres(Integer votesDem2024Pres) { this.votesDem2024Pres = votesDem2024Pres; }
    public Integer getVotesRep2024Pres() { return votesRep2024Pres; }
    public void setVotesRep2024Pres(Integer votesRep2024Pres) { this.votesRep2024Pres = votesRep2024Pres; }
    public Integer getVotesOther2024Pres() { return votesOther2024Pres; }
    public void setVotesOther2024Pres(Integer votesOther2024Pres) { this.votesOther2024Pres = votesOther2024Pres; }
    public Integer getTotalVotes2024Pres() { return totalVotes2024Pres; }
    public void setTotalVotes2024Pres(Integer totalVotes2024Pres) { this.totalVotes2024Pres = totalVotes2024Pres; }
    public Double getDemShare2024Pres() { return demShare2024Pres; }
    public void setDemShare2024Pres(Double demShare2024Pres) { this.demShare2024Pres = demShare2024Pres; }
    public Double getRepShare2024Pres() { return repShare2024Pres; }
    public void setRepShare2024Pres(Double repShare2024Pres) { this.repShare2024Pres = repShare2024Pres; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



