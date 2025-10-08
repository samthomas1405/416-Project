package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "census_blocks")
@CompoundIndex(name = "state_block_unique", def = "{'stateId':1,'blockId':1}", unique = true)
public class CensusBlock {
    @Id
    private String id;                 // "NY|360610001001001"
    private String stateId;
    private String blockId;            // GEOID
    private State.GeoPoint centroid;   // GeoJSON Point
    private String regionId;           // EAVS region mapping

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }
    public State.GeoPoint getCentroid() { return centroid; }
    public void setCentroid(State.GeoPoint centroid) { this.centroid = centroid; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
}
