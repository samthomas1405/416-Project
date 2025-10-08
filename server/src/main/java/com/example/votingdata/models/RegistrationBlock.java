package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "registration_blocks")
@CompoundIndexes({
    @CompoundIndex(name = "year_state_idx", def = "{'year':1,'stateId':1}"),
    @CompoundIndex(name = "year_state_block_unique", def = "{'year':1,'stateId':1,'blockId':1}", unique = true)
})
public class RegistrationBlock {
    @Id
    private String id;          // "2024|NY|360610001001001"
    private Integer year;
    private String stateId;
    private String blockId;
    private Counts counts;

    public static class Counts {
        private Integer DEM;
        private Integer REP;
        private Integer UNAFF;
        private Integer total;
        public Integer getDEM() { return DEM; }
        public void setDEM(Integer DEM) { this.DEM = DEM; }
        public Integer getREP() { return REP; }
        public void setREP(Integer REP) { this.REP = REP; }
        public Integer getUNAFF() { return UNAFF; }
        public void setUNAFF(Integer UNAFF) { this.UNAFF = UNAFF; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
    }

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }
    public Counts getCounts() { return counts; }
    public void setCounts(Counts counts) { this.counts = counts; }
}
