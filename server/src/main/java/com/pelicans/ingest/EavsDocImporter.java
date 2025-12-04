package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.EavsDoc;
import com.pelicans.model.GeoStateDoc;
import com.pelicans.model.StateDoc;
import com.pelicans.repository.EavsRepository;
import com.pelicans.repository.StateRepository;
import com.pelicans.repository.GeoStateRepository;
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

        // Pre-load state mappings into memory for fast lookups
        System.out.println("[EavsDocImporter] Pre-loading state mappings...");
        Map<String, Integer> stateFipsCache = new HashMap<>();
        stateRepository.findAll().forEach(state -> {
            try {
                Integer fips = Integer.parseInt(state.getStateFips());
                stateFipsCache.put(state.getStateAbbr().toUpperCase(), fips);
            } catch (NumberFormatException e) {
                // Skip invalid FIPS
            }
        });
        geoStateRepository.findAll().forEach(state -> {
            try {
                Integer fips = Integer.parseInt(state.getStateFips());
                stateFipsCache.put(state.getStateAbbr().toUpperCase(), fips);
            } catch (NumberFormatException e) {
                // Skip invalid FIPS
            }
        });
        System.out.println("[EavsDocImporter] Loaded " + stateFipsCache.size() + " state mappings");

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

        // Pre-filter and pre-categorize relevant column indices for faster processing
        // Map: column index -> target map type (1=totalRegistered, 2=sameDayRegistration, etc.)
        Map<Integer, Integer> colToMapType = new HashMap<>();
        Set<String> excludedHeaders = Set.of("FIPSCode", "Year", "year", "State", "state_abbr", "State_Abbr",
            "JurisdictionName", "Jurisdiction_Name", "jurisdiction_name", "fips5", "FIPS_2Digit",
            "MISSINGNESS_SCORE", "EQUIPMENT_QUALITY_SCORE", "A1a", "A1b", "A1c");
        
        // Pre-compute header upper case versions and categorize them
        String[] upperHeaders = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            upperHeaders[i] = headers[i].trim().toUpperCase();
        }
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            if (excludedHeaders.contains(header)) continue;
            
            String upperHeader = upperHeaders[i];
            if (upperHeader.length() == 0) continue;
            char prefix = upperHeader.charAt(0);
            
            if (prefix == 'A' || prefix == 'B' || prefix == 'C' || prefix == 'D' || prefix == 'E' || prefix == 'F') {
                // Categorize field type: 1-20 for different map types
                int mapType = categorizeField(upperHeader, prefix);
                if (mapType > 0) {
                    colToMapType.put(i, mapType);
                }
            }
        }

        int processed = 0;
        int skipped = 0;
        int errors = 0;
        
        // Batch processing for faster inserts - increased batch size for better performance
        List<EavsDoc> batch = new ArrayList<>();
        final int BATCH_SIZE = 1000;

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
                // Check multiple field name variations for jurisdiction name
                String jurisdictionName = getValue(row, colIndex, "JurisdictionName");
                if (jurisdictionName == null || jurisdictionName.isEmpty()) {
                    jurisdictionName = getValue(row, colIndex, "Jurisdiction_Name");
                }
                if (jurisdictionName == null || jurisdictionName.isEmpty()) {
                    jurisdictionName = getValue(row, colIndex, "jurisdiction_name");
                }
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

                // Look up stateFips from cache (much faster than database queries)
                String stateAbbrUpper = stateAbbr.toUpperCase();
                Integer stateFips = stateFipsCache.get(stateAbbrUpper);
                
                if (stateFips == null) {
                    System.err.println("[EavsDocImporter] Warning: Could not find stateFips for " + stateAbbrUpper + ", skipping");
                    skipped++;
                    continue;
                }
                
                // Only process 2024 documents for Massachusetts (stateFips 25)
                if (year != 2024 || stateFips != 25) {
                    skipped++;
                    continue;
                }

                // For Massachusetts (stateFips 25), use full FIPSCode since it reports at town/city level
                // For other states, use first 5 digits (county level)
                String fips5Value;
                if (stateFips == 25) {
                    // Use full FIPSCode for MA (remove decimal if present)
                    fips5Value = fipscode.replace(".0", "").replace(".", "");
                } else {
                    fips5Value = fips5 != null ? fips5 : fipscode.substring(0, Math.min(5, fipscode.length()));
                }
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
                
                // Always ensure A1a, A1b, A1c are present in totalRegistered (use -1 if empty)
                String a1aValue = getValue(row, colIndex, "A1a");
                String a1bValue = getValue(row, colIndex, "A1b");
                String a1cValue = getValue(row, colIndex, "A1c");
                
                totalRegistered.put("A1a", parseIntegerWithSentinels(a1aValue));
                totalRegistered.put("A1b", parseIntegerWithSentinels(a1bValue));
                totalRegistered.put("A1c", parseIntegerWithSentinels(a1cValue));
                
                // Process only relevant columns using pre-categorized map types for better performance
                for (Map.Entry<Integer, Integer> entry : colToMapType.entrySet()) {
                    int colIdx = entry.getKey();
                    if (colIdx >= row.length) continue;
                    int mapType = entry.getValue();
                    String value = row[colIdx];
                    if (value == null) continue;
                    
                    // Fast trim check - only trim if needed
                    int len = value.length();
                    int start = 0;
                    while (start < len && value.charAt(start) <= ' ') start++;
                    if (start == len) continue; // Empty after trimming
                    
                    // Parse value (trim inline if needed)
                    String trimmedValue = (start > 0 || len > 0 && value.charAt(len-1) > ' ') ? value.trim() : value;
                    Integer parsedValue = parseIntegerWithSentinels(trimmedValue);
                    
                    String header = headers[colIdx].trim();
                    
                    // Direct map assignment based on pre-computed type
                    switch (mapType) {
                        case 1: totalRegistered.put(header, parsedValue); break;
                        case 2: sameDayRegistration.put(header, parsedValue); break;
                        case 3: registrationMethods.put(header, parsedValue); break;
                        case 4: registrationUpdates.put(header, parsedValue); break;
                        case 5: registrationRemovals.put(header, parsedValue); break;
                        case 6: registrationCancellations.put(header, parsedValue); break;
                        case 7: registrationCorrections.put(header, parsedValue); break;
                        case 8: registrationTransfers.put(header, parsedValue); break;
                        case 9: registrationAdditions.put(header, parsedValue); break;
                        case 10: registrationChanges.put(header, parsedValue); break;
                        case 11: pollbookDeletions.put(header, parsedValue); break;
                        case 12: totalVotes.put(header, parsedValue); break; // Not used for 2024 (B1* goes to uocavaBallots)
                        case 13: electionDayVotes.put(header, parsedValue); break;
                        case 14: earlyVoting.put(header, parsedValue); break;
                        case 15: absenteeVoting.put(header, parsedValue); break;
                        case 16: earlyVotingTotals.put(header, parsedValue); break;
                        case 17: earlyVotingCategories.put(header, parsedValue); break;
                        case 18: earlyVotingInPerson.put(header, parsedValue); break;
                        case 19: earlyVotingByMail.put(header, parsedValue); break;
                        case 20: earlyVotingOther.put(header, parsedValue); break;
                        case 21: earlyVotingUocava.put(header, parsedValue); break;
                        case 22: earlyVotingDomestic.put(header, parsedValue); break;
                        case 23: earlyVotingOtherCategories.put(header, parsedValue); break;
                        case 24: earlyVotingTotals2.put(header, parsedValue); break;
                        case 25: uocavaBallots.put(header, parsedValue); break;
                        case 26: uocavaBallotsCounted.put(header, parsedValue); break;
                        case 27: uocavaBallotsRejected.put(header, parsedValue); break;
                        case 28: uocavaBallotsOther.put(header, parsedValue); break;
                        case 29: uocavaBallotsOtherCategories.put(header, parsedValue); break;
                        case 30: mailBallotsSent.put(header, parsedValue); break;
                        case 31: mailBallotApplications.put(header, parsedValue); break;
                        case 32: dropBoxReturns.put(header, parsedValue); break;
                        case 33: mailBallotsReturned.put(header, parsedValue); break;
                        case 34: mailBallotsCounted.put(header, parsedValue); break;
                        case 35: mailBallotsRejected.put(header, parsedValue); break;
                        case 36: provisionalBallotsCast.put(header, parsedValue); break;
                        case 37: provisionalBallotCategories.put(header, parsedValue); break;
                        case 38: equipmentInfo.put(header, parsedValue); break;
                        case 39: equipmentDetails.put(header, parsedValue); break;
                        case 40: equipmentCounts.put(header, parsedValue); break;
                        case 41: equipmentTypes.put(header, parsedValue); break;
                        case 42: equipmentAccessibility.put(header, parsedValue); break;
                        case 43: equipmentOther.put(header, parsedValue); break;
                        case 44: equipmentDetailed.put(header, parsedValue); break;
                        case 45: otherData.put(header, parsedValue); break;
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
                // Always set jurisdictionName (even if empty/null, it will be stored)
                provisional.setJurisdictionName(jurisdictionName != null ? jurisdictionName : "");
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
     * Text values like "Does not apply" or "-888888: Not Applicable" -> -1
     * Never returns null - always returns -1 for invalid/missing data
     */
    private Integer parseIntegerWithSentinels(String s) {
        if (s == null || s.isEmpty()) return -1;
        
        // Handle text values that indicate missing/not applicable data
        String trimmed = s.trim();
        if (trimmed.equalsIgnoreCase("does not apply") || 
            trimmed.equalsIgnoreCase("not applicable") ||
            trimmed.equalsIgnoreCase("n/a") ||
            trimmed.equalsIgnoreCase("na")) {
            return -1;
        }
        
        // Extract numeric part if there's text after a number (e.g., "-888888: Not Applicable")
        String numericPart = trimmed;
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex > 0) {
            numericPart = trimmed.substring(0, colonIndex).trim();
        }
        
        // Try to extract leading number using regex
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^-?\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(numericPart);
        if (matcher.find()) {
            numericPart = matcher.group();
        } else {
            // No numeric part found, return -1
            return -1;
        }
        
        try {
            int value = Integer.parseInt(numericPart);
            // Convert sentinel values to -1: -999999, -888888, or any negative number
            if (value < 0 || value == -999999 || value == -888888) {
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            // If it's not a number at all, return -1 for "not available" text values
            return -1;
        }
    }
    
    /**
     * Pre-categorize field into map type for faster processing.
     * Returns map type code (1-45) or 0 if not relevant.
     */
    private int categorizeField(String upperHeader, char prefix) {
        if (prefix == 'A') {
            if (upperHeader.startsWith("A1") && !upperHeader.startsWith("A10") && !upperHeader.startsWith("A11") && !upperHeader.startsWith("A12")) {
                if (!upperHeader.equals("A1A") && !upperHeader.equals("A1B") && !upperHeader.equals("A1C")) {
                    return 1; // totalRegistered
                }
            } else if (upperHeader.startsWith("A12")) return 11; // pollbookDeletions
            else if (upperHeader.startsWith("A3")) return 2; // sameDayRegistration
            else if (upperHeader.startsWith("A4")) return 3; // registrationMethods
            else if (upperHeader.startsWith("A5")) return 4; // registrationUpdates
            else if (upperHeader.startsWith("A6")) return 5; // registrationRemovals
            else if (upperHeader.startsWith("A7")) return 6; // registrationCancellations
            else if (upperHeader.startsWith("A8")) return 7; // registrationCorrections
            else if (upperHeader.startsWith("A9")) return 8; // registrationTransfers
            else if (upperHeader.startsWith("A10")) return 9; // registrationAdditions
            else if (upperHeader.startsWith("A11")) return 10; // registrationChanges
        } else if (prefix == 'B') {
            if (upperHeader.startsWith("B1")) return 25; // uocavaBallots (B1* = UOCAVA registration per 2024 codebook)
            else if (upperHeader.startsWith("B2")) return 13; // electionDayVotes (B2* = FPCA per 2024 codebook)
            else if (upperHeader.startsWith("B3")) return 14; // earlyVoting (B3* = Rejected FPCA per 2024 codebook)
            else if (upperHeader.startsWith("B4")) return 15; // absenteeVoting (B4* = Late FPCA per 2024 codebook)
            else if (upperHeader.startsWith("B5")) return 16; // earlyVotingTotals (B5* = UOCAVA Transmitted per 2024 codebook)
            else if (upperHeader.startsWith("B6")) return 17; // earlyVotingCategories (B6* = Post Mail Transmitted per 2024 codebook)
            else if (upperHeader.startsWith("B7")) return 18; // earlyVotingInPerson (B7* = Email Transmitted per 2024 codebook)
            else if (upperHeader.startsWith("B8")) return 19; // earlyVotingByMail (B8* = Fax Transmitted per 2024 codebook)
            else if (upperHeader.startsWith("B9")) return 20; // earlyVotingOther (B9* = Online Transmitted per 2024 codebook)
            else if (upperHeader.startsWith("B10")) return 21; // earlyVotingUocava (B10* = Other Transmitted per 2024 codebook)
            else if (upperHeader.startsWith("B11")) return 22; // earlyVotingDomestic (B11* = UOCAVA Returned per 2024 codebook)
            else if (upperHeader.startsWith("B12")) return 23; // earlyVotingOtherCategories (B12* = Post Mail Returned per 2024 codebook)
            else if (upperHeader.startsWith("B13")) return 24; // earlyVotingTotals2 (B13* = Email Returned per 2024 codebook)
            else if (upperHeader.startsWith("B14") || upperHeader.startsWith("B24")) return 25; // uocavaBallots (B14* = Fax Returned, B24* = UOCAVA Rejected per 2024 codebook)
            else if (upperHeader.startsWith("B15")) return 26; // uocavaBallotsCounted (B15* = Online Returned per 2024 codebook)
            else if (upperHeader.startsWith("B16")) return 27; // uocavaBallotsRejected (B16* = Other mode Returned per 2024 codebook)
            else if (upperHeader.startsWith("B17")) return 28; // uocavaBallotsOther (B17* = Total Undeliverable per 2024 codebook)
            else if (upperHeader.startsWith("B18")) return 29; // uocavaBallotsOtherCategories (B18* = UOCAVA Counted per 2024 codebook)
        } else if (prefix == 'C') {
            if (upperHeader.startsWith("C1")) return 30; // mailBallotsSent
            else if (upperHeader.startsWith("C2")) return 31; // mailBallotApplications
            else if (upperHeader.startsWith("C3")) return 32; // dropBoxReturns
            else if (upperHeader.startsWith("C4")) return 33; // mailBallotsReturned
            else if (upperHeader.startsWith("C5")) return 34; // mailBallotsCounted
            else if (upperHeader.startsWith("C6")) return 32; // dropBoxReturns (C6a is total drop box votes for 2024)
            else if (upperHeader.startsWith("C9")) return 35; // mailBallotsRejected
        } else if (prefix == 'E') {
            if (upperHeader.startsWith("E1")) return 36; // provisionalBallotsCast
            else if (upperHeader.startsWith("E2")) return 37; // provisionalBallotCategories
        } else if (prefix == 'F') {
            if (upperHeader.startsWith("F1")) return 38; // equipmentInfo
            else if (upperHeader.startsWith("F2")) return 39; // equipmentDetails
            else if (upperHeader.startsWith("F3")) return 40; // equipmentCounts
            else if (upperHeader.startsWith("F4")) return 41; // equipmentTypes
            else if (upperHeader.startsWith("F5")) return 42; // equipmentAccessibility
            else if (upperHeader.startsWith("F6")) return 43; // equipmentOther
            else if (upperHeader.startsWith("F7")) return 44; // equipmentDetailed
        } else if (prefix == 'D') {
            return 45; // otherData
        }
        return 0; // Not relevant
    }
}

