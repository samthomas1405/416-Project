package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.WaVoterDoc;
import com.pelicans.repository.WaVoterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "wa.voter.enabled", havingValue = "true", matchIfMissing = false)
public class WaVoterImporter implements CommandLineRunner {

    private final WaVoterRepository repo;

    @Value("${wa.voter.input:data_clean/registration/wa_vrdb_voters.csv}")
    private String inputPath;

    public WaVoterImporter(WaVoterRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[WaVoterImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[WaVoterImporter] Reading CSV: " + f.getAbsolutePath());
        System.out.println("[WaVoterImporter] WARNING: This is a large file (~5M rows). Import may take significant time.");

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[WaVoterImporter] CSV is empty");
            return;
        }

        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = buildColumnIndex(headers);

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
                String stateVoterId = getValue(row, colIndex, "state_voter_id");
                if (stateVoterId == null || stateVoterId.isEmpty()) {
                    skipped++;
                    continue;
                }

                String docId = stateVoterId;

                WaVoterDoc voter = new WaVoterDoc();
                voter.setId(docId);
                voter.setStateVoterId(stateVoterId);
                voter.setBirthyear(parseInt(getValue(row, colIndex, "birthyear")));
                voter.setAge2024(parseInt(getValue(row, colIndex, "age_2024")));
                voter.setAgeGroup2024(getValue(row, colIndex, "age_group_2024"));
                voter.setGender(getValue(row, colIndex, "gender"));
                voter.setCountyCode(getValue(row, colIndex, "county_code"));
                voter.setCountyName(getValue(row, colIndex, "county_name"));
                voter.setPrecinctCode(getValue(row, colIndex, "precinct_code"));
                voter.setPrecinctPart(getValue(row, colIndex, "precinct_part"));
                voter.setLegislativeDistrict(getValue(row, colIndex, "legislative_district"));
                voter.setCongressionalDistrict(getValue(row, colIndex, "congressional_district"));
                voter.setRegistrationDate(getValue(row, colIndex, "registration_date"));
                voter.setLastVoted(getValue(row, colIndex, "last_voted"));
                voter.setStatusCode(getValue(row, colIndex, "status_code"));

                repo.save(voter);
                processed++;

                if (processed % 1000 == 0) {
                    System.out.println("[WaVoterImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[WaVoterImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[WaVoterImporter] Import complete:");
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



