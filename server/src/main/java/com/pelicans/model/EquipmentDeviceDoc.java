package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "EquipmentDevice")
@CompoundIndex(name = "equipment_unique", def = "{'stateAbbr':1,'fipsCode':1,'manufacturer':1,'model':1,'equipmentType':1}", unique = true)
public class EquipmentDeviceDoc {
    @Id
    private String id;  // stateAbbr|fipsCode|manufacturer|model|equipmentType
    private String stateAbbr;
    private String fipsCode;
    private String stateName;
    private String jurisdiction;
    private String equipmentType;
    private String manufacturer;
    private String model;
    private Integer firstYearInUse;
    private String barcode;
    private String vppat;
    private Boolean electionDayStandard;
    private Boolean electionDayAccessible;
    private Boolean earlyVotingStandard;
    private Boolean earlyVotingAccessible;
    private Boolean mailBallotEquipment;
    private String extraText;
    private Double qualityScore;
    private Date createdAt;
    private Date updatedAt;

    public EquipmentDeviceDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
    public String getFipsCode() { return fipsCode; }
    public void setFipsCode(String fipsCode) { this.fipsCode = fipsCode; }
    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getFirstYearInUse() { return firstYearInUse; }
    public void setFirstYearInUse(Integer firstYearInUse) { this.firstYearInUse = firstYearInUse; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getVppat() { return vppat; }
    public void setVppat(String vppat) { this.vppat = vppat; }
    public Boolean getElectionDayStandard() { return electionDayStandard; }
    public void setElectionDayStandard(Boolean electionDayStandard) { this.electionDayStandard = electionDayStandard; }
    public Boolean getElectionDayAccessible() { return electionDayAccessible; }
    public void setElectionDayAccessible(Boolean electionDayAccessible) { this.electionDayAccessible = electionDayAccessible; }
    public Boolean getEarlyVotingStandard() { return earlyVotingStandard; }
    public void setEarlyVotingStandard(Boolean earlyVotingStandard) { this.earlyVotingStandard = earlyVotingStandard; }
    public Boolean getEarlyVotingAccessible() { return earlyVotingAccessible; }
    public void setEarlyVotingAccessible(Boolean earlyVotingAccessible) { this.earlyVotingAccessible = earlyVotingAccessible; }
    public Boolean getMailBallotEquipment() { return mailBallotEquipment; }
    public void setMailBallotEquipment(Boolean mailBallotEquipment) { this.mailBallotEquipment = mailBallotEquipment; }
    public String getExtraText() { return extraText; }
    public void setExtraText(String extraText) { this.extraText = extraText; }
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}



