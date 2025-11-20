package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "Eavs")
@CompoundIndex(name = "year_state_fips_unique", def = "{'year':1,'stateFips':1,'fips5':1}", unique = true)
@CompoundIndex(name = "year_state_idx", def = "{'year':1,'stateFips':1}")
@CompoundIndex(name = "year_fips5_idx", def = "{'year':1,'fips5':1}")
public class EavsDoc {
    @Id
    private String id;  // year|stateFips|fips5
    private Integer year;
    private Integer stateFips;  // Primary identifier (2-digit FIPS code)
    private String jurisdictionName;
    private String fipscode;  // Original 9-digit EAVS code
    private String fips5;     // 5-digit county FIPS
    private Double missingnessScore;
    private Double equipmentQualityScore;
    
    // Organized categories for easier querying
    private Registration registration;
    private Voting voting;
    private MailBallots mailBallots;
    private Provisional provisional;
    private Equipment equipment;
    private Other other;
    
    private Date createdAt;
    private Date updatedAt;

    public EavsDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Nested classes for organized data
    public static class Registration {
        // A1* - Total registered voters (counts - integers)
        private Map<String, Integer> totalRegistered;  // A1A, A1B, A1C
        // A3* - Same-day registration (counts - integers)
        private Map<String, Integer> sameDayRegistration;  // A3A, A3B, A3C
        // A4* - Registration methods (counts - integers)
        private Map<String, Integer> registrationMethods;  // A4*
        // A5* - Registration updates (counts - integers)
        private Map<String, Integer> registrationUpdates;  // A5*
        // A6* - Registration removals (counts - integers)
        private Map<String, Integer> registrationRemovals;  // A6*
        // A7* - Registration cancellations (counts - integers)
        private Map<String, Integer> registrationCancellations;  // A7*
        // A8* - Registration corrections (counts - integers)
        private Map<String, Integer> registrationCorrections;  // A8*
        // A9* - Registration transfers (counts - integers)
        private Map<String, Integer> registrationTransfers;  // A9*
        // A10* - Registration additions (counts - integers)
        private Map<String, Integer> registrationAdditions;  // A10*
        // A11* - Registration changes (counts - integers)
        private Map<String, Integer> registrationChanges;  // A11*
        // A12* - Pollbook deletions (counts - integers)
        private Map<String, Integer> pollbookDeletions;  // A12A, A12B, ..., A12H
        
        // Getters and setters
        public Map<String, Integer> getTotalRegistered() { return totalRegistered; }
        public void setTotalRegistered(Map<String, Integer> totalRegistered) { this.totalRegistered = totalRegistered; }
        public Map<String, Integer> getSameDayRegistration() { return sameDayRegistration; }
        public void setSameDayRegistration(Map<String, Integer> sameDayRegistration) { this.sameDayRegistration = sameDayRegistration; }
        public Map<String, Integer> getRegistrationMethods() { return registrationMethods; }
        public void setRegistrationMethods(Map<String, Integer> registrationMethods) { this.registrationMethods = registrationMethods; }
        public Map<String, Integer> getRegistrationUpdates() { return registrationUpdates; }
        public void setRegistrationUpdates(Map<String, Integer> registrationUpdates) { this.registrationUpdates = registrationUpdates; }
        public Map<String, Integer> getRegistrationRemovals() { return registrationRemovals; }
        public void setRegistrationRemovals(Map<String, Integer> registrationRemovals) { this.registrationRemovals = registrationRemovals; }
        public Map<String, Integer> getRegistrationCancellations() { return registrationCancellations; }
        public void setRegistrationCancellations(Map<String, Integer> registrationCancellations) { this.registrationCancellations = registrationCancellations; }
        public Map<String, Integer> getRegistrationCorrections() { return registrationCorrections; }
        public void setRegistrationCorrections(Map<String, Integer> registrationCorrections) { this.registrationCorrections = registrationCorrections; }
        public Map<String, Integer> getRegistrationTransfers() { return registrationTransfers; }
        public void setRegistrationTransfers(Map<String, Integer> registrationTransfers) { this.registrationTransfers = registrationTransfers; }
        public Map<String, Integer> getRegistrationAdditions() { return registrationAdditions; }
        public void setRegistrationAdditions(Map<String, Integer> registrationAdditions) { this.registrationAdditions = registrationAdditions; }
        public Map<String, Integer> getRegistrationChanges() { return registrationChanges; }
        public void setRegistrationChanges(Map<String, Integer> registrationChanges) { this.registrationChanges = registrationChanges; }
        public Map<String, Integer> getPollbookDeletions() { return pollbookDeletions; }
        public void setPollbookDeletions(Map<String, Integer> pollbookDeletions) { this.pollbookDeletions = pollbookDeletions; }
    }

    public static class Voting {
        // B1* - Total votes cast (counts - integers)
        private Map<String, Integer> totalVotes;  // B1A, B1B, B1C, etc.
        // B2* - Election day votes (counts - integers)
        private Map<String, Integer> electionDayVotes;  // B2*
        // B3* - Early voting (counts - integers)
        private Map<String, Integer> earlyVoting;  // B3*
        // B4* - Absentee voting (counts - integers)
        private Map<String, Integer> absenteeVoting;  // B4*
        // B5* - Early voting totals (counts - integers)
        private Map<String, Integer> earlyVotingTotals;  // B5A, B5B, B5C
        // B6* - Early voting categories (counts - integers)
        private Map<String, Integer> earlyVotingCategories;  // B6A, B6B, B6C
        // B7* - Early voting in-person (counts - integers)
        private Map<String, Integer> earlyVotingInPerson;  // B7*
        // B8* - Early voting by mail (counts - integers)
        private Map<String, Integer> earlyVotingByMail;  // B8*
        // B9* - Early voting other (counts - integers)
        private Map<String, Integer> earlyVotingOther;  // B9*
        // B10* - Early voting UOCAVA (counts - integers)
        private Map<String, Integer> earlyVotingUocava;  // B10*
        // B11* - Early voting domestic (counts - integers)
        private Map<String, Integer> earlyVotingDomestic;  // B11*
        // B12* - Early voting other categories (counts - integers)
        private Map<String, Integer> earlyVotingOtherCategories;  // B12*
        // B13* - Early voting totals (counts - integers)
        private Map<String, Integer> earlyVotingTotals2;  // B13*
        // B14* - UOCAVA ballots (counts - integers)
        private Map<String, Integer> uocavaBallots;  // B14*, B24A
        // B15* - UOCAVA ballots counted (counts - integers)
        private Map<String, Integer> uocavaBallotsCounted;  // B15*
        // B16* - UOCAVA ballots rejected (counts - integers)
        private Map<String, Integer> uocavaBallotsRejected;  // B16*
        // B17* - UOCAVA ballots other (counts - integers)
        private Map<String, Integer> uocavaBallotsOther;  // B17*
        // B18* - UOCAVA ballots other categories (counts - integers)
        private Map<String, Integer> uocavaBallotsOtherCategories;  // B18*
        
        // Getters and setters
        public Map<String, Integer> getTotalVotes() { return totalVotes; }
        public void setTotalVotes(Map<String, Integer> totalVotes) { this.totalVotes = totalVotes; }
        public Map<String, Integer> getElectionDayVotes() { return electionDayVotes; }
        public void setElectionDayVotes(Map<String, Integer> electionDayVotes) { this.electionDayVotes = electionDayVotes; }
        public Map<String, Integer> getEarlyVoting() { return earlyVoting; }
        public void setEarlyVoting(Map<String, Integer> earlyVoting) { this.earlyVoting = earlyVoting; }
        public Map<String, Integer> getAbsenteeVoting() { return absenteeVoting; }
        public void setAbsenteeVoting(Map<String, Integer> absenteeVoting) { this.absenteeVoting = absenteeVoting; }
        public Map<String, Integer> getEarlyVotingTotals() { return earlyVotingTotals; }
        public void setEarlyVotingTotals(Map<String, Integer> earlyVotingTotals) { this.earlyVotingTotals = earlyVotingTotals; }
        public Map<String, Integer> getEarlyVotingCategories() { return earlyVotingCategories; }
        public void setEarlyVotingCategories(Map<String, Integer> earlyVotingCategories) { this.earlyVotingCategories = earlyVotingCategories; }
        public Map<String, Integer> getEarlyVotingInPerson() { return earlyVotingInPerson; }
        public void setEarlyVotingInPerson(Map<String, Integer> earlyVotingInPerson) { this.earlyVotingInPerson = earlyVotingInPerson; }
        public Map<String, Integer> getEarlyVotingByMail() { return earlyVotingByMail; }
        public void setEarlyVotingByMail(Map<String, Integer> earlyVotingByMail) { this.earlyVotingByMail = earlyVotingByMail; }
        public Map<String, Integer> getEarlyVotingOther() { return earlyVotingOther; }
        public void setEarlyVotingOther(Map<String, Integer> earlyVotingOther) { this.earlyVotingOther = earlyVotingOther; }
        public Map<String, Integer> getEarlyVotingUocava() { return earlyVotingUocava; }
        public void setEarlyVotingUocava(Map<String, Integer> earlyVotingUocava) { this.earlyVotingUocava = earlyVotingUocava; }
        public Map<String, Integer> getEarlyVotingDomestic() { return earlyVotingDomestic; }
        public void setEarlyVotingDomestic(Map<String, Integer> earlyVotingDomestic) { this.earlyVotingDomestic = earlyVotingDomestic; }
        public Map<String, Integer> getEarlyVotingOtherCategories() { return earlyVotingOtherCategories; }
        public void setEarlyVotingOtherCategories(Map<String, Integer> earlyVotingOtherCategories) { this.earlyVotingOtherCategories = earlyVotingOtherCategories; }
        public Map<String, Integer> getEarlyVotingTotals2() { return earlyVotingTotals2; }
        public void setEarlyVotingTotals2(Map<String, Integer> earlyVotingTotals2) { this.earlyVotingTotals2 = earlyVotingTotals2; }
        public Map<String, Integer> getUocavaBallots() { return uocavaBallots; }
        public void setUocavaBallots(Map<String, Integer> uocavaBallots) { this.uocavaBallots = uocavaBallots; }
        public Map<String, Integer> getUocavaBallotsCounted() { return uocavaBallotsCounted; }
        public void setUocavaBallotsCounted(Map<String, Integer> uocavaBallotsCounted) { this.uocavaBallotsCounted = uocavaBallotsCounted; }
        public Map<String, Integer> getUocavaBallotsRejected() { return uocavaBallotsRejected; }
        public void setUocavaBallotsRejected(Map<String, Integer> uocavaBallotsRejected) { this.uocavaBallotsRejected = uocavaBallotsRejected; }
        public Map<String, Integer> getUocavaBallotsOther() { return uocavaBallotsOther; }
        public void setUocavaBallotsOther(Map<String, Integer> uocavaBallotsOther) { this.uocavaBallotsOther = uocavaBallotsOther; }
        public Map<String, Integer> getUocavaBallotsOtherCategories() { return uocavaBallotsOtherCategories; }
        public void setUocavaBallotsOtherCategories(Map<String, Integer> uocavaBallotsOtherCategories) { this.uocavaBallotsOtherCategories = uocavaBallotsOtherCategories; }
    }

    public static class MailBallots {
        // C1* - Mail ballots sent/requested (counts - integers)
        private Map<String, Integer> mailBallotsSent;  // C1A, C1B, C1C, etc.
        // C2* - Mail ballot applications (counts - integers)
        private Map<String, Integer> mailBallotApplications;  // C2*
        // C3* - Drop box returns (counts - integers)
        private Map<String, Integer> dropBoxReturns;  // C3A
        // C4* - Mail ballots returned (counts - integers)
        private Map<String, Integer> mailBallotsReturned;  // C4*
        // C5* - Mail ballots counted (counts - integers)
        private Map<String, Integer> mailBallotsCounted;  // C5*
        // C9* - Mail ballots rejected (counts - integers)
        private Map<String, Integer> mailBallotsRejected;  // C9A, C9B, ..., C9Q
        
        // Getters and setters
        public Map<String, Integer> getMailBallotsSent() { return mailBallotsSent; }
        public void setMailBallotsSent(Map<String, Integer> mailBallotsSent) { this.mailBallotsSent = mailBallotsSent; }
        public Map<String, Integer> getMailBallotApplications() { return mailBallotApplications; }
        public void setMailBallotApplications(Map<String, Integer> mailBallotApplications) { this.mailBallotApplications = mailBallotApplications; }
        public Map<String, Integer> getDropBoxReturns() { return dropBoxReturns; }
        public void setDropBoxReturns(Map<String, Integer> dropBoxReturns) { this.dropBoxReturns = dropBoxReturns; }
        public Map<String, Integer> getMailBallotsReturned() { return mailBallotsReturned; }
        public void setMailBallotsReturned(Map<String, Integer> mailBallotsReturned) { this.mailBallotsReturned = mailBallotsReturned; }
        public Map<String, Integer> getMailBallotsCounted() { return mailBallotsCounted; }
        public void setMailBallotsCounted(Map<String, Integer> mailBallotsCounted) { this.mailBallotsCounted = mailBallotsCounted; }
        public Map<String, Integer> getMailBallotsRejected() { return mailBallotsRejected; }
        public void setMailBallotsRejected(Map<String, Integer> mailBallotsRejected) { this.mailBallotsRejected = mailBallotsRejected; }
    }

    public static class Provisional {
        private String jurisdictionName;  // Also stored here for convenience
        // E1* - Provisional ballots cast (counts - integers)
        private Map<String, Integer> provisionalBallotsCast;  // E1A, E1B, E1C, E1D
        // E2* - Provisional ballot categories (counts - integers)
        private Map<String, Integer> provisionalBallotCategories;  // E2A, E2B, ..., E2I
        
        // Getters and setters
        public String getJurisdictionName() { return jurisdictionName; }
        public void setJurisdictionName(String jurisdictionName) { this.jurisdictionName = jurisdictionName; }
        public Map<String, Integer> getProvisionalBallotsCast() { return provisionalBallotsCast; }
        public void setProvisionalBallotsCast(Map<String, Integer> provisionalBallotsCast) { this.provisionalBallotsCast = provisionalBallotsCast; }
        public Map<String, Integer> getProvisionalBallotCategories() { return provisionalBallotCategories; }
        public void setProvisionalBallotCategories(Map<String, Integer> provisionalBallotCategories) { this.provisionalBallotCategories = provisionalBallotCategories; }
    }

    public static class Equipment {
        // F1* - Equipment information (counts - integers)
        private Map<String, Integer> equipmentInfo;  // F1*
        // F2* - Equipment details (counts - integers)
        private Map<String, Integer> equipmentDetails;  // F2*
        // F3* - Equipment counts (counts - integers)
        private Map<String, Integer> equipmentCounts;  // F3*
        // F4* - Equipment types (counts - integers)
        private Map<String, Integer> equipmentTypes;  // F4*
        // F5* - Equipment accessibility (counts - integers)
        private Map<String, Integer> equipmentAccessibility;  // F5*
        // F6* - Equipment other (counts - integers)
        private Map<String, Integer> equipmentOther;  // F6*
        // F7* - Equipment detailed breakdown (counts - integers)
        private Map<String, Integer> equipmentDetailed;  // F7*
        
        // Getters and setters
        public Map<String, Integer> getEquipmentInfo() { return equipmentInfo; }
        public void setEquipmentInfo(Map<String, Integer> equipmentInfo) { this.equipmentInfo = equipmentInfo; }
        public Map<String, Integer> getEquipmentDetails() { return equipmentDetails; }
        public void setEquipmentDetails(Map<String, Integer> equipmentDetails) { this.equipmentDetails = equipmentDetails; }
        public Map<String, Integer> getEquipmentCounts() { return equipmentCounts; }
        public void setEquipmentCounts(Map<String, Integer> equipmentCounts) { this.equipmentCounts = equipmentCounts; }
        public Map<String, Integer> getEquipmentTypes() { return equipmentTypes; }
        public void setEquipmentTypes(Map<String, Integer> equipmentTypes) { this.equipmentTypes = equipmentTypes; }
        public Map<String, Integer> getEquipmentAccessibility() { return equipmentAccessibility; }
        public void setEquipmentAccessibility(Map<String, Integer> equipmentAccessibility) { this.equipmentAccessibility = equipmentAccessibility; }
        public Map<String, Integer> getEquipmentOther() { return equipmentOther; }
        public void setEquipmentOther(Map<String, Integer> equipmentOther) { this.equipmentOther = equipmentOther; }
        public Map<String, Integer> getEquipmentDetailed() { return equipmentDetailed; }
        public void setEquipmentDetailed(Map<String, Integer> equipmentDetailed) { this.equipmentDetailed = equipmentDetailed; }
    }

    public static class Other {
        // D1* - Other categories (counts - integers)
        private Map<String, Integer> otherData;  // D1*, D2*, D3*, D4*, D5*
        
        // Getters and setters
        public Map<String, Integer> getOtherData() { return otherData; }
        public void setOtherData(Map<String, Integer> otherData) { this.otherData = otherData; }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getStateFips() { return stateFips; }
    public void setStateFips(Integer stateFips) { this.stateFips = stateFips; }
    public String getJurisdictionName() { return jurisdictionName; }
    public void setJurisdictionName(String jurisdictionName) { this.jurisdictionName = jurisdictionName; }
    public String getFipscode() { return fipscode; }
    public void setFipscode(String fipscode) { this.fipscode = fipscode; }
    public String getFips5() { return fips5; }
    public void setFips5(String fips5) { this.fips5 = fips5; }
    public Double getMissingnessScore() { return missingnessScore; }
    public void setMissingnessScore(Double missingnessScore) { this.missingnessScore = missingnessScore; }
    public Double getEquipmentQualityScore() { return equipmentQualityScore; }
    public void setEquipmentQualityScore(Double equipmentQualityScore) { this.equipmentQualityScore = equipmentQualityScore; }
    public Registration getRegistration() { return registration; }
    public void setRegistration(Registration registration) { this.registration = registration; }
    public Voting getVoting() { return voting; }
    public void setVoting(Voting voting) { this.voting = voting; }
    public MailBallots getMailBallots() { return mailBallots; }
    public void setMailBallots(MailBallots mailBallots) { this.mailBallots = mailBallots; }
    public Provisional getProvisional() { return provisional; }
    public void setProvisional(Provisional provisional) { this.provisional = provisional; }
    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }
    public Other getOther() { return other; }
    public void setOther(Other other) { this.other = other; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

