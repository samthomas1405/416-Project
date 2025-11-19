package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.WaRegistrationAgeDoc;
import com.pelicans.repository.WaRegistrationAgeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "wa.registration.county.age.enabled", havingValue = "true", matchIfMissing = false)
public class WaRegistrationCountyAgeImporter implements CommandLineRunner {

    private final WaRegistrationAgeRepository repo;

    @Value("${wa.registration.county.age.input:data_clean/registration/wa_registration_by_county_age.csv}")
    private String inputPath;

    public WaRegistrationCountyAgeImporter(WaRegistrationAgeRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[WaRegistrationCountyAgeImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[WaRegistrationCountyAgeImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[WaRegistrationCountyAgeImporter] CSV is empty");
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
                String stateAbbr = getValue(row, colIndex, "state_abbr");
                String countyName = getValue(row, colIndex, "county_name");
                String ageGroup = getValue(row, colIndex, "age_group");

                if (stateAbbr == null || countyName == null || ageGroup == null) {
                    skipped++;
                    continue;
                }

                String docId = stateAbbr.toUpperCase() + "|" + countyName.toUpperCase() + "|" + ageGroup;

                WaRegistrationAgeDoc registration = new WaRegistrationAgeDoc();
                registration.setId(docId);
                registration.setStateAbbr(stateAbbr.toUpperCase());
                registration.setCountyName(countyName);
                registration.setAgeGroup(ageGroup);
                registration.setRegisteredVoters(parseInt(getValue(row, colIndex, "registered_voters")));

                repo.save(registration);
                processed++;

                if (processed % 50 == 0) {
                    System.out.println("[WaRegistrationCountyAgeImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[WaRegistrationCountyAgeImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[WaRegistrationCountyAgeImporter] Import complete:");
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



