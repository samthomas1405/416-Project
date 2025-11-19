package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.EavsDoc;
import com.pelicans.repository.EavsRepository;
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

    @Value("${eavs.all.input:data_clean/eavs/eavs_2016_2024_normalized.csv}")
    private String inputPath;

    public EavsDocImporter(EavsRepository repo) {
        this.repo = repo;
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

                String docId = year + "|" + stateAbbr + "|" + (fips5 != null ? fips5 : fipscode.substring(0, Math.min(5, fipscode.length())));

                EavsDoc eavs = new EavsDoc();
                eavs.setId(docId);
                eavs.setYear(year);
                eavs.setStateAbbr(stateAbbr.toUpperCase());
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
                
                // Registration maps
                Map<String, Object> totalRegistered = new HashMap<>();
                Map<String, Object> sameDayRegistration = new HashMap<>();
                Map<String, Object> registrationMethods = new HashMap<>();
                Map<String, Object> registrationUpdates = new HashMap<>();
                Map<String, Object> registrationRemovals = new HashMap<>();
                Map<String, Object> registrationCancellations = new HashMap<>();
                Map<String, Object> registrationCorrections = new HashMap<>();
                Map<String, Object> registrationTransfers = new HashMap<>();
                Map<String, Object> registrationAdditions = new HashMap<>();
                Map<String, Object> registrationChanges = new HashMap<>();
                Map<String, Object> pollbookDeletions = new HashMap<>();
                
                // Voting maps
                Map<String, Object> totalVotes = new HashMap<>();
                Map<String, Object> electionDayVotes = new HashMap<>();
                Map<String, Object> earlyVoting = new HashMap<>();
                Map<String, Object> absenteeVoting = new HashMap<>();
                Map<String, Object> earlyVotingTotals = new HashMap<>();
                Map<String, Object> earlyVotingCategories = new HashMap<>();
                Map<String, Object> earlyVotingInPerson = new HashMap<>();
                Map<String, Object> earlyVotingByMail = new HashMap<>();
                Map<String, Object> earlyVotingOther = new HashMap<>();
                Map<String, Object> earlyVotingUocava = new HashMap<>();
                Map<String, Object> earlyVotingDomestic = new HashMap<>();
                Map<String, Object> earlyVotingOtherCategories = new HashMap<>();
                Map<String, Object> earlyVotingTotals2 = new HashMap<>();
                Map<String, Object> uocavaBallots = new HashMap<>();
                Map<String, Object> uocavaBallotsCounted = new HashMap<>();
                Map<String, Object> uocavaBallotsRejected = new HashMap<>();
                Map<String, Object> uocavaBallotsOther = new HashMap<>();
                Map<String, Object> uocavaBallotsOtherCategories = new HashMap<>();
                
                // Mail ballots maps
                Map<String, Object> mailBallotsSent = new HashMap<>();
                Map<String, Object> mailBallotApplications = new HashMap<>();
                Map<String, Object> dropBoxReturns = new HashMap<>();
                Map<String, Object> mailBallotsReturned = new HashMap<>();
                Map<String, Object> mailBallotsCounted = new HashMap<>();
                Map<String, Object> mailBallotsRejected = new HashMap<>();
                
                // Provisional maps
                Map<String, Object> provisionalBallotsCast = new HashMap<>();
                Map<String, Object> provisionalBallotCategories = new HashMap<>();
                
                // Equipment maps
                Map<String, Object> equipmentInfo = new HashMap<>();
                Map<String, Object> equipmentDetails = new HashMap<>();
                Map<String, Object> equipmentCounts = new HashMap<>();
                Map<String, Object> equipmentTypes = new HashMap<>();
                Map<String, Object> equipmentAccessibility = new HashMap<>();
                Map<String, Object> equipmentOther = new HashMap<>();
                Map<String, Object> equipmentDetailed = new HashMap<>();
                
                // Other maps
                Map<String, Object> otherData = new HashMap<>();
                
                for (String header : headers) {
                    if (!header.equals("FIPSCode") && !header.equals("Year") && 
                        !header.equals("year") && !header.equals("State") && 
                        !header.equals("state_abbr") && !header.equals("State_Abbr") &&
                        !header.equals("JurisdictionName") && !header.equals("jurisdiction_name") &&
                        !header.equals("fips5") && !header.equals("FIPS_2Digit") &&
                        !header.equals("MISSINGNESS_SCORE") && !header.equals("EQUIPMENT_QUALITY_SCORE")) {
                        String value = getValue(row, colIndex, header);
                        if (value != null && !value.isEmpty()) {
                            // Try to parse as number, otherwise store as string
                            Object parsedValue = parseNumericOrString(value);
                            
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
                // Note: questions map is not saved - only organized structure is stored

                // Missingness and equipment quality scores
                String missingnessStr = getValue(row, colIndex, "MISSINGNESS_SCORE");
                if (missingnessStr != null && !missingnessStr.isEmpty()) {
                    eavs.setMissingnessScore(parseDouble(missingnessStr));
                }
                String equipmentStr = getValue(row, colIndex, "EQUIPMENT_QUALITY_SCORE");
                if (equipmentStr != null && !equipmentStr.isEmpty()) {
                    eavs.setEquipmentQualityScore(parseDouble(equipmentStr));
                }

                repo.save(eavs);
                processed++;

                if (processed % 100 == 0) {
                    System.out.println("[EavsDocImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[EavsDocImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
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

    private Object parseNumericOrString(String s) {
        if (s == null || s.isEmpty()) return null;
        // Try integer first
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            // Try double
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e2) {
                // Return as string
                return s;
            }
        }
    }
}

