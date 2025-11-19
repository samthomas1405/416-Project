package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "Eavs")
@CompoundIndex(name = "year_state_fips_unique", def = "{'year':1,'stateAbbr':1,'fips5':1}", unique = true)
@CompoundIndex(name = "year_state_idx", def = "{'year':1,'stateAbbr':1}")
@CompoundIndex(name = "year_fips5_idx", def = "{'year':1,'fips5':1}")
public class EavsDoc {
    @Id
    private String id;  // year|stateAbbr|fips5
    private Integer year;
    private String stateAbbr;
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
    
    // Keep raw questions map for backward compatibility and fields not in categories
    private Map<String, Object> questions;
    
    private Date createdAt;
    private Date updatedAt;

    public EavsDoc() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Nested classes for organized data
    public static class Registration {
        // A1* - Total registered voters
        private Map<String, Object> totalRegistered;  // A1A, A1B, A1C
        // A3* - Same-day registration
        private Map<String, Object> sameDayRegistration;  // A3A, A3B, A3C
        // A4* - Registration methods
        private Map<String, Object> registrationMethods;  // A4*
        // A5* - Registration updates
        private Map<String, Object> registrationUpdates;  // A5*
        // A6* - Registration removals
        private Map<String, Object> registrationRemovals;  // A6*
        // A7* - Registration cancellations
        private Map<String, Object> registrationCancellations;  // A7*
        // A8* - Registration corrections
        private Map<String, Object> registrationCorrections;  // A8*
        // A9* - Registration transfers
        private Map<String, Object> registrationTransfers;  // A9*
        // A10* - Registration additions
        private Map<String, Object> registrationAdditions;  // A10*
        // A11* - Registration changes
        private Map<String, Object> registrationChanges;  // A11*
        // A12* - Pollbook deletions
        private Map<String, Object> pollbookDeletions;  // A12A, A12B, ..., A12H
        
        // Getters and setters
        public Map<String, Object> getTotalRegistered() { return totalRegistered; }
        public void setTotalRegistered(Map<String, Object> totalRegistered) { this.totalRegistered = totalRegistered; }
        public Map<String, Object> getSameDayRegistration() { return sameDayRegistration; }
        public void setSameDayRegistration(Map<String, Object> sameDayRegistration) { this.sameDayRegistration = sameDayRegistration; }
        public Map<String, Object> getRegistrationMethods() { return registrationMethods; }
        public void setRegistrationMethods(Map<String, Object> registrationMethods) { this.registrationMethods = registrationMethods; }
        public Map<String, Object> getRegistrationUpdates() { return registrationUpdates; }
        public void setRegistrationUpdates(Map<String, Object> registrationUpdates) { this.registrationUpdates = registrationUpdates; }
        public Map<String, Object> getRegistrationRemovals() { return registrationRemovals; }
        public void setRegistrationRemovals(Map<String, Object> registrationRemovals) { this.registrationRemovals = registrationRemovals; }
        public Map<String, Object> getRegistrationCancellations() { return registrationCancellations; }
        public void setRegistrationCancellations(Map<String, Object> registrationCancellations) { this.registrationCancellations = registrationCancellations; }
        public Map<String, Object> getRegistrationCorrections() { return registrationCorrections; }
        public void setRegistrationCorrections(Map<String, Object> registrationCorrections) { this.registrationCorrections = registrationCorrections; }
        public Map<String, Object> getRegistrationTransfers() { return registrationTransfers; }
        public void setRegistrationTransfers(Map<String, Object> registrationTransfers) { this.registrationTransfers = registrationTransfers; }
        public Map<String, Object> getRegistrationAdditions() { return registrationAdditions; }
        public void setRegistrationAdditions(Map<String, Object> registrationAdditions) { this.registrationAdditions = registrationAdditions; }
        public Map<String, Object> getRegistrationChanges() { return registrationChanges; }
        public void setRegistrationChanges(Map<String, Object> registrationChanges) { this.registrationChanges = registrationChanges; }
        public Map<String, Object> getPollbookDeletions() { return pollbookDeletions; }
        public void setPollbookDeletions(Map<String, Object> pollbookDeletions) { this.pollbookDeletions = pollbookDeletions; }
    }

    public static class Voting {
        // B1* - Total votes cast
        private Map<String, Object> totalVotes;  // B1A, B1B, B1C, etc.
        // B2* - Election day votes
        private Map<String, Object> electionDayVotes;  // B2*
        // B3* - Early voting
        private Map<String, Object> earlyVoting;  // B3*
        // B4* - Absentee voting
        private Map<String, Object> absenteeVoting;  // B4*
        // B5* - Early voting totals
        private Map<String, Object> earlyVotingTotals;  // B5A, B5B, B5C
        // B6* - Early voting categories
        private Map<String, Object> earlyVotingCategories;  // B6A, B6B, B6C
        // B7* - Early voting in-person
        private Map<String, Object> earlyVotingInPerson;  // B7*
        // B8* - Early voting by mail
        private Map<String, Object> earlyVotingByMail;  // B8*
        // B9* - Early voting other
        private Map<String, Object> earlyVotingOther;  // B9*
        // B10* - Early voting UOCAVA
        private Map<String, Object> earlyVotingUocava;  // B10*
        // B11* - Early voting domestic
        private Map<String, Object> earlyVotingDomestic;  // B11*
        // B12* - Early voting other categories
        private Map<String, Object> earlyVotingOtherCategories;  // B12*
        // B13* - Early voting totals
        private Map<String, Object> earlyVotingTotals2;  // B13*
        // B14* - UOCAVA ballots
        private Map<String, Object> uocavaBallots;  // B14*, B24A
        // B15* - UOCAVA ballots counted
        private Map<String, Object> uocavaBallotsCounted;  // B15*
        // B16* - UOCAVA ballots rejected
        private Map<String, Object> uocavaBallotsRejected;  // B16*
        // B17* - UOCAVA ballots other
        private Map<String, Object> uocavaBallotsOther;  // B17*
        // B18* - UOCAVA ballots other categories
        private Map<String, Object> uocavaBallotsOtherCategories;  // B18*
        
        // Getters and setters
        public Map<String, Object> getTotalVotes() { return totalVotes; }
        public void setTotalVotes(Map<String, Object> totalVotes) { this.totalVotes = totalVotes; }
        public Map<String, Object> getElectionDayVotes() { return electionDayVotes; }
        public void setElectionDayVotes(Map<String, Object> electionDayVotes) { this.electionDayVotes = electionDayVotes; }
        public Map<String, Object> getEarlyVoting() { return earlyVoting; }
        public void setEarlyVoting(Map<String, Object> earlyVoting) { this.earlyVoting = earlyVoting; }
        public Map<String, Object> getAbsenteeVoting() { return absenteeVoting; }
        public void setAbsenteeVoting(Map<String, Object> absenteeVoting) { this.absenteeVoting = absenteeVoting; }
        public Map<String, Object> getEarlyVotingTotals() { return earlyVotingTotals; }
        public void setEarlyVotingTotals(Map<String, Object> earlyVotingTotals) { this.earlyVotingTotals = earlyVotingTotals; }
        public Map<String, Object> getEarlyVotingCategories() { return earlyVotingCategories; }
        public void setEarlyVotingCategories(Map<String, Object> earlyVotingCategories) { this.earlyVotingCategories = earlyVotingCategories; }
        public Map<String, Object> getEarlyVotingInPerson() { return earlyVotingInPerson; }
        public void setEarlyVotingInPerson(Map<String, Object> earlyVotingInPerson) { this.earlyVotingInPerson = earlyVotingInPerson; }
        public Map<String, Object> getEarlyVotingByMail() { return earlyVotingByMail; }
        public void setEarlyVotingByMail(Map<String, Object> earlyVotingByMail) { this.earlyVotingByMail = earlyVotingByMail; }
        public Map<String, Object> getEarlyVotingOther() { return earlyVotingOther; }
        public void setEarlyVotingOther(Map<String, Object> earlyVotingOther) { this.earlyVotingOther = earlyVotingOther; }
        public Map<String, Object> getEarlyVotingUocava() { return earlyVotingUocava; }
        public void setEarlyVotingUocava(Map<String, Object> earlyVotingUocava) { this.earlyVotingUocava = earlyVotingUocava; }
        public Map<String, Object> getEarlyVotingDomestic() { return earlyVotingDomestic; }
        public void setEarlyVotingDomestic(Map<String, Object> earlyVotingDomestic) { this.earlyVotingDomestic = earlyVotingDomestic; }
        public Map<String, Object> getEarlyVotingOtherCategories() { return earlyVotingOtherCategories; }
        public void setEarlyVotingOtherCategories(Map<String, Object> earlyVotingOtherCategories) { this.earlyVotingOtherCategories = earlyVotingOtherCategories; }
        public Map<String, Object> getEarlyVotingTotals2() { return earlyVotingTotals2; }
        public void setEarlyVotingTotals2(Map<String, Object> earlyVotingTotals2) { this.earlyVotingTotals2 = earlyVotingTotals2; }
        public Map<String, Object> getUocavaBallots() { return uocavaBallots; }
        public void setUocavaBallots(Map<String, Object> uocavaBallots) { this.uocavaBallots = uocavaBallots; }
        public Map<String, Object> getUocavaBallotsCounted() { return uocavaBallotsCounted; }
        public void setUocavaBallotsCounted(Map<String, Object> uocavaBallotsCounted) { this.uocavaBallotsCounted = uocavaBallotsCounted; }
        public Map<String, Object> getUocavaBallotsRejected() { return uocavaBallotsRejected; }
        public void setUocavaBallotsRejected(Map<String, Object> uocavaBallotsRejected) { this.uocavaBallotsRejected = uocavaBallotsRejected; }
        public Map<String, Object> getUocavaBallotsOther() { return uocavaBallotsOther; }
        public void setUocavaBallotsOther(Map<String, Object> uocavaBallotsOther) { this.uocavaBallotsOther = uocavaBallotsOther; }
        public Map<String, Object> getUocavaBallotsOtherCategories() { return uocavaBallotsOtherCategories; }
        public void setUocavaBallotsOtherCategories(Map<String, Object> uocavaBallotsOtherCategories) { this.uocavaBallotsOtherCategories = uocavaBallotsOtherCategories; }
    }

    public static class MailBallots {
        // C1* - Mail ballots sent/requested
        private Map<String, Object> mailBallotsSent;  // C1A, C1B, C1C, etc.
        // C2* - Mail ballot applications
        private Map<String, Object> mailBallotApplications;  // C2*
        // C3* - Drop box returns
        private Map<String, Object> dropBoxReturns;  // C3A
        // C4* - Mail ballots returned
        private Map<String, Object> mailBallotsReturned;  // C4*
        // C5* - Mail ballots counted
        private Map<String, Object> mailBallotsCounted;  // C5*
        // C9* - Mail ballots rejected
        private Map<String, Object> mailBallotsRejected;  // C9A, C9B, ..., C9Q
        
        // Getters and setters
        public Map<String, Object> getMailBallotsSent() { return mailBallotsSent; }
        public void setMailBallotsSent(Map<String, Object> mailBallotsSent) { this.mailBallotsSent = mailBallotsSent; }
        public Map<String, Object> getMailBallotApplications() { return mailBallotApplications; }
        public void setMailBallotApplications(Map<String, Object> mailBallotApplications) { this.mailBallotApplications = mailBallotApplications; }
        public Map<String, Object> getDropBoxReturns() { return dropBoxReturns; }
        public void setDropBoxReturns(Map<String, Object> dropBoxReturns) { this.dropBoxReturns = dropBoxReturns; }
        public Map<String, Object> getMailBallotsReturned() { return mailBallotsReturned; }
        public void setMailBallotsReturned(Map<String, Object> mailBallotsReturned) { this.mailBallotsReturned = mailBallotsReturned; }
        public Map<String, Object> getMailBallotsCounted() { return mailBallotsCounted; }
        public void setMailBallotsCounted(Map<String, Object> mailBallotsCounted) { this.mailBallotsCounted = mailBallotsCounted; }
        public Map<String, Object> getMailBallotsRejected() { return mailBallotsRejected; }
        public void setMailBallotsRejected(Map<String, Object> mailBallotsRejected) { this.mailBallotsRejected = mailBallotsRejected; }
    }

    public static class Provisional {
        // E1* - Provisional ballots cast
        private Map<String, Object> provisionalBallotsCast;  // E1A, E1B, E1C, E1D
        // E2* - Provisional ballot categories
        private Map<String, Object> provisionalBallotCategories;  // E2A, E2B, ..., E2I
        
        // Getters and setters
        public Map<String, Object> getProvisionalBallotsCast() { return provisionalBallotsCast; }
        public void setProvisionalBallotsCast(Map<String, Object> provisionalBallotsCast) { this.provisionalBallotsCast = provisionalBallotsCast; }
        public Map<String, Object> getProvisionalBallotCategories() { return provisionalBallotCategories; }
        public void setProvisionalBallotCategories(Map<String, Object> provisionalBallotCategories) { this.provisionalBallotCategories = provisionalBallotCategories; }
    }

    public static class Equipment {
        // F1* - Equipment information
        private Map<String, Object> equipmentInfo;  // F1*
        // F2* - Equipment details
        private Map<String, Object> equipmentDetails;  // F2*
        // F3* - Equipment counts
        private Map<String, Object> equipmentCounts;  // F3*
        // F4* - Equipment types
        private Map<String, Object> equipmentTypes;  // F4*
        // F5* - Equipment accessibility
        private Map<String, Object> equipmentAccessibility;  // F5*
        // F6* - Equipment other
        private Map<String, Object> equipmentOther;  // F6*
        // F7* - Equipment detailed breakdown
        private Map<String, Object> equipmentDetailed;  // F7*
        
        // Getters and setters
        public Map<String, Object> getEquipmentInfo() { return equipmentInfo; }
        public void setEquipmentInfo(Map<String, Object> equipmentInfo) { this.equipmentInfo = equipmentInfo; }
        public Map<String, Object> getEquipmentDetails() { return equipmentDetails; }
        public void setEquipmentDetails(Map<String, Object> equipmentDetails) { this.equipmentDetails = equipmentDetails; }
        public Map<String, Object> getEquipmentCounts() { return equipmentCounts; }
        public void setEquipmentCounts(Map<String, Object> equipmentCounts) { this.equipmentCounts = equipmentCounts; }
        public Map<String, Object> getEquipmentTypes() { return equipmentTypes; }
        public void setEquipmentTypes(Map<String, Object> equipmentTypes) { this.equipmentTypes = equipmentTypes; }
        public Map<String, Object> getEquipmentAccessibility() { return equipmentAccessibility; }
        public void setEquipmentAccessibility(Map<String, Object> equipmentAccessibility) { this.equipmentAccessibility = equipmentAccessibility; }
        public Map<String, Object> getEquipmentOther() { return equipmentOther; }
        public void setEquipmentOther(Map<String, Object> equipmentOther) { this.equipmentOther = equipmentOther; }
        public Map<String, Object> getEquipmentDetailed() { return equipmentDetailed; }
        public void setEquipmentDetailed(Map<String, Object> equipmentDetailed) { this.equipmentDetailed = equipmentDetailed; }
    }

    public static class Other {
        // D1* - Other categories
        private Map<String, Object> otherData;  // D1*, D2*, D3*, D4*, D5*
        
        // Getters and setters
        public Map<String, Object> getOtherData() { return otherData; }
        public void setOtherData(Map<String, Object> otherData) { this.otherData = otherData; }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getStateAbbr() { return stateAbbr; }
    public void setStateAbbr(String stateAbbr) { this.stateAbbr = stateAbbr; }
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
    public Map<String, Object> getQuestions() { return questions; }
    public void setQuestions(Map<String, Object> questions) { this.questions = questions; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

