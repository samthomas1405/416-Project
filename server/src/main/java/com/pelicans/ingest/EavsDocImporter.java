package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.EavsDoc;
import com.pelicans.model.GeoStateDoc;
import com.pelicans.model.StateDoc;
import com.pelicans.repository.EavsRepository;
import com.pelicans.repository.GeoStateRepository;
import com.pelicans.repository.StateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "eavs.all.enabled", havingValue = "true", matchIfMissing = false)
public class EavsDocImporter implements CommandLineRunner {

    private final EavsRepository repo;
    private final StateRepository stateRepository;
    private final GeoStateRepository geoStateRepository;

    @Value("${eavs.all.input:data_clean/eavs/eavs_2016_2024_normalized.csv}")
    private String inputPath;

    public EavsDocImporter(EavsRepository repo, StateRepository stateRepository, GeoStateRepository geoStateRepository) {
        this.repo = repo;
        this.stateRepository = stateRepository;
        this.geoStateRepository = geoStateRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[EavsDocImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[EavsDocImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[EavsDocImporter] CSV is empty");
            return;
        }

        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim(), i);
        }

        int processed = 0;
        int skipped = 0;
        int errors = 0;
        
        // Batch processing for faster inserts
        List<EavsDoc> batch = new ArrayList<>();
        final int BATCH_SIZE = 500;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < headers.length) {
                skipped++;
                continue;
            }

            try {
                String fipscode = getValue(row, colIndex, "FIPSCode");
                String yearStr = getValue(row, colIndex, "year");
                if (yearStr == null) {
                    yearStr = getValue(row, colIndex, "Year");  // Try capitalized version
                }
                String stateAbbr = getValue(row, colIndex, "state_abbr");
                if (stateAbbr == null) {
                    stateAbbr = getValue(row, colIndex, "State_Abbr");  // Try capitalized version
                }
                if (stateAbbr == null) {
                    stateAbbr = getValue(row, colIndex, "State");  // Fallback to State column
                }
                String jurisdictionName = getValue(row, colIndex, "JurisdictionName");
                String fips5 = getValue(row, colIndex, "fips5");

                if (fipscode == null || yearStr == null || stateAbbr == null) {
                    skipped++;
                    continue;
                }

                // Skip state-level aggregations
                if (fips5 != null && fips5.endsWith("000")) {
                    skipped++;
                    continue;
                }

                Integer year = parseInt(yearStr);
                if (year == null) {
                    skipped++;
                    continue;
                }

                // Look up stateFips from stateAbbr
                String stateAbbrUpper = stateAbbr.toUpperCase();
                String stateFips = null;
                Optional<StateDoc> stateOpt = stateRepository.findByStateAbbr(stateAbbrUpper);
                if (stateOpt.isPresent()) {
                    stateFips = stateOpt.get().getStateFips();
                } else {
                    Optional<GeoStateDoc> geoStateOpt = geoStateRepository.findByStateAbbr(stateAbbrUpper);
                    if (geoStateOpt.isPresent()) {
                        stateFips = geoStateOpt.get().getStateFips();
                    }
                }

                if (stateFips == null || stateFips.isEmpty()) {
                    System.err.println("[EavsDocImporter] Warning: Could not find stateFips for " + stateAbbrUpper + ", skipping");
                    skipped++;
                    continue;
                }

                String fips5Value = fips5 != null ? fips5 : fipscode.substring(0, Math.min(5, fipscode.length()));
                String docId = year + "|" + stateFips + "|" + fips5Value;

                EavsDoc eavs = new EavsDoc();
                eavs.setId(docId);
                eavs.setYear(year);
                eavs.setStateFips(stateFips);
                eavs.setJurisdictionName(jurisdictionName);
                eavs.setFipscode(fipscode);
                eavs.setFips5(fips5);

                // Organize fields into categories
                EavsDoc.Registration registration = new EavsDoc.Registration();
                EavsDoc.Voting voting = new EavsDoc.Voting();
                EavsDoc.MailBallots mailBallots = new EavsDoc.MailBallots();
                EavsDoc.Provisional provisional = new EavsDoc.Provisional();
                EavsDoc.Equipment equipment = new EavsDoc.Equipment();
                EavsDoc.Other other = new EavsDoc.Other();
                
                // Registration maps (Integer for counts)
                Map<String, Integer> totalRegistered = new HashMap<>();
                Map<String, Integer> sameDayRegistration = new HashMap<>();
                Map<String, Integer> registrationMethods = new HashMap<>();
                Map<String, Integer> registrationUpdates = new HashMap<>();
                Map<String, Integer> registrationRemovals = new HashMap<>();
                Map<String, Integer> registrationCancellations = new HashMap<>();
                Map<String, Integer> registrationCorrections = new HashMap<>();
                Map<String, Integer> registrationTransfers = new HashMap<>();
                Map<String, Integer> registrationAdditions = new HashMap<>();
                Map<String, Integer> registrationChanges = new HashMap<>();
                Map<String, Integer> pollbookDeletions = new HashMap<>();
                
                // Voting maps (Integer for counts)
                Map<String, Integer> totalVotes = new HashMap<>();
                Map<String, Integer> electionDayVotes = new HashMap<>();
                Map<String, Integer> earlyVoting = new HashMap<>();
                Map<String, Integer> absenteeVoting = new HashMap<>();
                Map<String, Integer> earlyVotingTotals = new HashMap<>();
                Map<String, Integer> earlyVotingCategories = new HashMap<>();
                Map<String, Integer> earlyVotingInPerson = new HashMap<>();
                Map<String, Integer> earlyVotingByMail = new HashMap<>();
                Map<String, Integer> earlyVotingOther = new HashMap<>();
                Map<String, Integer> earlyVotingUocava = new HashMap<>();
                Map<String, Integer> earlyVotingDomestic = new HashMap<>();
                Map<String, Integer> earlyVotingOtherCategories = new HashMap<>();
                Map<String, Integer> earlyVotingTotals2 = new HashMap<>();
                Map<String, Integer> uocavaBallots = new HashMap<>();
                Map<String, Integer> uocavaBallotsCounted = new HashMap<>();
                Map<String, Integer> uocavaBallotsRejected = new HashMap<>();
                Map<String, Integer> uocavaBallotsOther = new HashMap<>();
                Map<String, Integer> uocavaBallotsOtherCategories = new HashMap<>();
                
                // Mail ballots maps (Integer for counts)
                Map<String, Integer> mailBallotsSent = new HashMap<>();
                Map<String, Integer> mailBallotApplications = new HashMap<>();
                Map<String, Integer> dropBoxReturns = new HashMap<>();
                Map<String, Integer> mailBallotsReturned = new HashMap<>();
                Map<String, Integer> mailBallotsCounted = new HashMap<>();
                Map<String, Integer> mailBallotsRejected = new HashMap<>();
                
                // Provisional maps (Integer for counts)
                Map<String, Integer> provisionalBallotsCast = new HashMap<>();
                Map<String, Integer> provisionalBallotCategories = new HashMap<>();
                
                // Equipment maps (Integer for counts)
                Map<String, Integer> equipmentInfo = new HashMap<>();
                Map<String, Integer> equipmentDetails = new HashMap<>();
                Map<String, Integer> equipmentCounts = new HashMap<>();
                Map<String, Integer> equipmentTypes = new HashMap<>();
                Map<String, Integer> equipmentAccessibility = new HashMap<>();
                Map<String, Integer> equipmentOther = new HashMap<>();
                Map<String, Integer> equipmentDetailed = new HashMap<>();
                
                // Other maps (Integer for counts)
                Map<String, Integer> otherData = new HashMap<>();
                
                for (String header : headers) {
                    if (!header.equals("FIPSCode") && !header.equals("Year") && 
                        !header.equals("year") && !header.equals("State") && 
                        !header.equals("state_abbr") && !header.equals("State_Abbr") &&
                        !header.equals("JurisdictionName") && !header.equals("jurisdiction_name") &&
                        !header.equals("fips5") && !header.equals("FIPS_2Digit") &&
                        !header.equals("MISSINGNESS_SCORE") && !header.equals("EQUIPMENT_QUALITY_SCORE")) {
                        String value = getValue(row, colIndex, header);
                        if (value != null && !value.isEmpty()) {
                            // Parse as Integer for count fields, convert sentinel values to -1
                            Integer parsedValue = parseIntegerWithSentinels(value);
                            if (parsedValue == null) continue; // Skip only invalid data (non-numeric)
                            
                            // Organize into categories based on field prefix
                            String upperHeader = header.toUpperCase();
                            char prefix = upperHeader.length() > 0 ? upperHeader.charAt(0) : 'X';
                            String numPart = upperHeader.length() > 1 ? upperHeader.substring(1).replaceAll("[^0-9]", "") : "";
                            
                            if (prefix == 'A') {
                                // Registration fields
                                if (upperHeader.startsWith("A1") && !upperHeader.startsWith("A10") && !upperHeader.startsWith("A11") && !upperHeader.startsWith("A12")) {
                                    totalRegistered.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A12")) {
                                    pollbookDeletions.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A3")) {
                                    sameDayRegistration.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A4")) {
                                    registrationMethods.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A5")) {
                                    registrationUpdates.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A6")) {
                                    registrationRemovals.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A7")) {
                                    registrationCancellations.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A8")) {
                                    registrationCorrections.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A9")) {
                                    registrationTransfers.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A10")) {
                                    registrationAdditions.put(header, parsedValue);
                                } else if (upperHeader.startsWith("A11")) {
                                    registrationChanges.put(header, parsedValue);
                                }
                            } else if (prefix == 'B') {
                                // Voting fields
                                if (upperHeader.startsWith("B1")) {
                                    totalVotes.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B2")) {
                                    electionDayVotes.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B3")) {
                                    earlyVoting.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B4")) {
                                    absenteeVoting.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B5")) {
                                    earlyVotingTotals.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B6")) {
                                    earlyVotingCategories.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B7")) {
                                    earlyVotingInPerson.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B8")) {
                                    earlyVotingByMail.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B9")) {
                                    earlyVotingOther.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B10")) {
                                    earlyVotingUocava.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B11")) {
                                    earlyVotingDomestic.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B12")) {
                                    earlyVotingOtherCategories.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B13")) {
                                    earlyVotingTotals2.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B14") || upperHeader.startsWith("B24")) {
                                    uocavaBallots.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B15")) {
                                    uocavaBallotsCounted.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B16")) {
                                    uocavaBallotsRejected.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B17")) {
                                    uocavaBallotsOther.put(header, parsedValue);
                                } else if (upperHeader.startsWith("B18")) {
                                    uocavaBallotsOtherCategories.put(header, parsedValue);
                                }
                            } else if (prefix == 'C') {
                                // Mail ballots
                                if (upperHeader.startsWith("C1")) {
                                    mailBallotsSent.put(header, parsedValue);
                                } else if (upperHeader.startsWith("C2")) {
                                    mailBallotApplications.put(header, parsedValue);
                                } else if (upperHeader.startsWith("C3")) {
                                    dropBoxReturns.put(header, parsedValue);
                                } else if (upperHeader.startsWith("C4")) {
                                    mailBallotsReturned.put(header, parsedValue);
                                } else if (upperHeader.startsWith("C5")) {
                                    mailBallotsCounted.put(header, parsedValue);
                                } else if (upperHeader.startsWith("C9")) {
                                    mailBallotsRejected.put(header, parsedValue);
                                }
                            } else if (prefix == 'E') {
                                // Provisional
                                if (upperHeader.startsWith("E1")) {
                                    provisionalBallotsCast.put(header, parsedValue);
                                } else if (upperHeader.startsWith("E2")) {
                                    provisionalBallotCategories.put(header, parsedValue);
                                }
                            } else if (prefix == 'F') {
                                // Equipment
                                if (upperHeader.startsWith("F1")) {
                                    equipmentInfo.put(header, parsedValue);
                                } else if (upperHeader.startsWith("F2")) {
                                    equipmentDetails.put(header, parsedValue);
                                } else if (upperHeader.startsWith("F3")) {
                                    equipmentCounts.put(header, parsedValue);
                                } else if (upperHeader.startsWith("F4")) {
                                    equipmentTypes.put(header, parsedValue);
                                } else if (upperHeader.startsWith("F5")) {
                                    equipmentAccessibility.put(header, parsedValue);
                                } else if (upperHeader.startsWith("F6")) {
                                    equipmentOther.put(header, parsedValue);
                                } else if (upperHeader.startsWith("F7")) {
                                    equipmentDetailed.put(header, parsedValue);
                                }
                            } else if (prefix == 'D') {
                                // Other
                                otherData.put(header, parsedValue);
                            }
                        }
                    }
                }
                
                // Set organized categories - Registration
                if (!totalRegistered.isEmpty()) registration.setTotalRegistered(totalRegistered);
                if (!sameDayRegistration.isEmpty()) registration.setSameDayRegistration(sameDayRegistration);
                if (!registrationMethods.isEmpty()) registration.setRegistrationMethods(registrationMethods);
                if (!registrationUpdates.isEmpty()) registration.setRegistrationUpdates(registrationUpdates);
                if (!registrationRemovals.isEmpty()) registration.setRegistrationRemovals(registrationRemovals);
                if (!registrationCancellations.isEmpty()) registration.setRegistrationCancellations(registrationCancellations);
                if (!registrationCorrections.isEmpty()) registration.setRegistrationCorrections(registrationCorrections);
                if (!registrationTransfers.isEmpty()) registration.setRegistrationTransfers(registrationTransfers);
                if (!registrationAdditions.isEmpty()) registration.setRegistrationAdditions(registrationAdditions);
                if (!registrationChanges.isEmpty()) registration.setRegistrationChanges(registrationChanges);
                if (!pollbookDeletions.isEmpty()) registration.setPollbookDeletions(pollbookDeletions);
                
                // Set organized categories - Voting
                if (!totalVotes.isEmpty()) voting.setTotalVotes(totalVotes);
                if (!electionDayVotes.isEmpty()) voting.setElectionDayVotes(electionDayVotes);
                if (!earlyVoting.isEmpty()) voting.setEarlyVoting(earlyVoting);
                if (!absenteeVoting.isEmpty()) voting.setAbsenteeVoting(absenteeVoting);
                if (!earlyVotingTotals.isEmpty()) voting.setEarlyVotingTotals(earlyVotingTotals);
                if (!earlyVotingCategories.isEmpty()) voting.setEarlyVotingCategories(earlyVotingCategories);
                if (!earlyVotingInPerson.isEmpty()) voting.setEarlyVotingInPerson(earlyVotingInPerson);
                if (!earlyVotingByMail.isEmpty()) voting.setEarlyVotingByMail(earlyVotingByMail);
                if (!earlyVotingOther.isEmpty()) voting.setEarlyVotingOther(earlyVotingOther);
                if (!earlyVotingUocava.isEmpty()) voting.setEarlyVotingUocava(earlyVotingUocava);
                if (!earlyVotingDomestic.isEmpty()) voting.setEarlyVotingDomestic(earlyVotingDomestic);
                if (!earlyVotingOtherCategories.isEmpty()) voting.setEarlyVotingOtherCategories(earlyVotingOtherCategories);
                if (!earlyVotingTotals2.isEmpty()) voting.setEarlyVotingTotals2(earlyVotingTotals2);
                if (!uocavaBallots.isEmpty()) voting.setUocavaBallots(uocavaBallots);
                if (!uocavaBallotsCounted.isEmpty()) voting.setUocavaBallotsCounted(uocavaBallotsCounted);
                if (!uocavaBallotsRejected.isEmpty()) voting.setUocavaBallotsRejected(uocavaBallotsRejected);
                if (!uocavaBallotsOther.isEmpty()) voting.setUocavaBallotsOther(uocavaBallotsOther);
                if (!uocavaBallotsOtherCategories.isEmpty()) voting.setUocavaBallotsOtherCategories(uocavaBallotsOtherCategories);
                
                // Set organized categories - Mail Ballots
                if (!mailBallotsSent.isEmpty()) mailBallots.setMailBallotsSent(mailBallotsSent);
                if (!mailBallotApplications.isEmpty()) mailBallots.setMailBallotApplications(mailBallotApplications);
                if (!dropBoxReturns.isEmpty()) mailBallots.setDropBoxReturns(dropBoxReturns);
                if (!mailBallotsReturned.isEmpty()) mailBallots.setMailBallotsReturned(mailBallotsReturned);
                if (!mailBallotsCounted.isEmpty()) mailBallots.setMailBallotsCounted(mailBallotsCounted);
                if (!mailBallotsRejected.isEmpty()) mailBallots.setMailBallotsRejected(mailBallotsRejected);
                
                // Set organized categories - Provisional
                if (jurisdictionName != null) {
                    provisional.setJurisdictionName(jurisdictionName);
                }
                if (!provisionalBallotsCast.isEmpty()) provisional.setProvisionalBallotsCast(provisionalBallotsCast);
                if (!provisionalBallotCategories.isEmpty()) provisional.setProvisionalBallotCategories(provisionalBallotCategories);
                
                // Set organized categories - Equipment
                if (!equipmentInfo.isEmpty()) equipment.setEquipmentInfo(equipmentInfo);
                if (!equipmentDetails.isEmpty()) equipment.setEquipmentDetails(equipmentDetails);
                if (!equipmentCounts.isEmpty()) equipment.setEquipmentCounts(equipmentCounts);
                if (!equipmentTypes.isEmpty()) equipment.setEquipmentTypes(equipmentTypes);
                if (!equipmentAccessibility.isEmpty()) equipment.setEquipmentAccessibility(equipmentAccessibility);
                if (!equipmentOther.isEmpty()) equipment.setEquipmentOther(equipmentOther);
                if (!equipmentDetailed.isEmpty()) equipment.setEquipmentDetailed(equipmentDetailed);
                
                // Set organized categories - Other
                if (!otherData.isEmpty()) other.setOtherData(otherData);
                
                eavs.setRegistration(registration);
                eavs.setVoting(voting);
                eavs.setMailBallots(mailBallots);
                eavs.setProvisional(provisional);
                eavs.setEquipment(equipment);
                eavs.setOther(other);

                // Missingness and equipment quality scores
                String missingnessStr = getValue(row, colIndex, "MISSINGNESS_SCORE");
                if (missingnessStr != null && !missingnessStr.isEmpty()) {
                    eavs.setMissingnessScore(parseDouble(missingnessStr));
                }
                String equipmentStr = getValue(row, colIndex, "EQUIPMENT_QUALITY_SCORE");
                if (equipmentStr != null && !equipmentStr.isEmpty()) {
                    eavs.setEquipmentQualityScore(parseDouble(equipmentStr));
                }

                // Add to batch instead of saving immediately
                batch.add(eavs);
                processed++;

                // Save batch when it reaches BATCH_SIZE
                if (batch.size() >= BATCH_SIZE) {
                    repo.saveAll(batch);
                    batch.clear();
                    System.out.println("[EavsDocImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[EavsDocImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }
        
        // Save remaining batch
        if (!batch.isEmpty()) {
            repo.saveAll(batch);
            System.out.println("[EavsDocImporter] Saved final batch: " + batch.size() + " records");
        }

        System.out.println("[EavsDocImporter] Import complete:");
        System.out.println("  Processed: " + processed);
        System.out.println("  Skipped: " + skipped);
        System.out.println("  Errors: " + errors);
    }

    private String getValue(String[] row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null || idx >= row.length) return null;
        String val = row[idx];
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    private Integer parseInt(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse as Integer, converting EAVS sentinel values to -1:
     * -999999: Data Not Available -> -1
     * -888888: Not Applicable -> -1
     * Any other negative number -> -1
     * Returns null only for invalid data (non-numeric strings)
     */
    private Integer parseIntegerWithSentinels(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            int value = Integer.parseInt(s);
            // Convert sentinel values to -1: -999999, -888888, or any negative number
            if (value < 0 || value == -999999 || value == -888888) {
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

