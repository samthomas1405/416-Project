package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.StateDoc;
import com.pelicans.model.WaVotersPartyAggregatedDoc;
import com.pelicans.repository.StateRepository;
import com.pelicans.repository.WaVotersPartyAggregatedRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "wa.voters.party.aggregated.enabled", havingValue = "true", matchIfMissing = false)
public class WaVotersPartyAggregatedImporter implements CommandLineRunner {

    private final WaVotersPartyAggregatedRepository repo;
    private final StateRepository stateRepository;

    @Value("${wa.voters.party.aggregated.input:data_clean/gui/gui19_wa_voters_party_aggregated.csv}")
    private String inputPath;

    public WaVotersPartyAggregatedImporter(WaVotersPartyAggregatedRepository repo, StateRepository stateRepository) {
        this.repo = repo;
        this.stateRepository = stateRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[WaVotersPartyAggregatedImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        // Look up WA stateFips
        Integer waStateFips = null;
        Optional<StateDoc> waState = stateRepository.findById("WA");
        if (waState.isPresent()) {
            try {
                waStateFips = Integer.parseInt(waState.get().getStateFips());
            } catch (NumberFormatException e) {
                System.err.println("[WaVotersPartyAggregatedImporter] Error parsing WA stateFips: " + waState.get().getStateFips());
            }
        }
        
        if (waStateFips == null) {
            System.err.println("[WaVotersPartyAggregatedImporter] Could not find WA stateFips, defaulting to 53");
            waStateFips = 53; // Washington state FIPS code
        }
        
        System.out.println("[WaVotersPartyAggregatedImporter] Using stateFips: " + waStateFips);
        System.out.println("[WaVotersPartyAggregatedImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[WaVotersPartyAggregatedImporter] CSV is empty");
            return;
        }

        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = buildColumnIndex(headers);

        int processed = 0;
        int skipped = 0;
        int errors = 0;

        List<WaVotersPartyAggregatedDoc> batch = new ArrayList<>();
        final int BATCH_SIZE = 500;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < headers.length) {
                skipped++;
                continue;
            }

            try {
                String countyCode = getValue(row, colIndex, "county_code");
                String countyName = getValue(row, colIndex, "county_name");
                String countyMajorityParty = getValue(row, colIndex, "county_majority_party");
                String ageGroup2024 = getValue(row, colIndex, "age_group_2024");
                String gender = getValue(row, colIndex, "gender");
                String statusCode = getValue(row, colIndex, "status_code");
                String voterCountStr = getValue(row, colIndex, "voter_count");

                if (countyCode == null || countyName == null || countyMajorityParty == null || 
                    ageGroup2024 == null || gender == null || statusCode == null || voterCountStr == null) {
                    skipped++;
                    continue;
                }

                // Build composite ID: stateFips|countyCode|countyMajorityParty|ageGroup2024|gender|statusCode
                String docId = waStateFips + "|" + countyCode.toUpperCase() + "|" + countyMajorityParty.toUpperCase() + 
                              "|" + ageGroup2024 + "|" + gender.toUpperCase() + "|" + statusCode;

                WaVotersPartyAggregatedDoc doc = new WaVotersPartyAggregatedDoc();
                doc.setId(docId);
                doc.setStateFips(waStateFips);
                doc.setCountyCode(countyCode.toUpperCase());
                doc.setCountyName(countyName);
                doc.setCountyMajorityParty(countyMajorityParty.toUpperCase());
                doc.setAgeGroup2024(ageGroup2024);
                doc.setGender(gender);
                doc.setStatusCode(statusCode);
                doc.setVoterCount(parseInt(voterCountStr));

                batch.add(doc);

                if (batch.size() >= BATCH_SIZE) {
                    repo.saveAll(batch);
                    processed += batch.size();
                    batch.clear();
                    System.out.println("[WaVotersPartyAggregatedImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[WaVotersPartyAggregatedImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        // Save remaining batch
        if (!batch.isEmpty()) {
            repo.saveAll(batch);
            processed += batch.size();
        }

        System.out.println("[WaVotersPartyAggregatedImporter] Import complete:");
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

    private Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }
        return colIndex;
    }
}

