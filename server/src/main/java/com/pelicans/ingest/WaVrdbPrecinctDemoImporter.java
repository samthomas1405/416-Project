package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.WaDemographicsPrecinctDoc;
import com.pelicans.repository.WaDemographicsPrecinctRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "wa.vrdb.precinct.demo.enabled", havingValue = "true", matchIfMissing = false)
public class WaVrdbPrecinctDemoImporter implements CommandLineRunner {

    private final WaDemographicsPrecinctRepository repo;

    @Value("${wa.vrdb.precinct.demo.input:data_clean/registration/wa_vrdb_precinct_age_gender_summary_2024.csv}")
    private String inputPath;

    public WaVrdbPrecinctDemoImporter(WaDemographicsPrecinctRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[WaVrdbPrecinctDemoImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[WaVrdbPrecinctDemoImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[WaVrdbPrecinctDemoImporter] CSV is empty");
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
                String countyCode = getValue(row, colIndex, "county_code");
                String precinctCode = getValue(row, colIndex, "precinct_code");
                String precinctPart = getValue(row, colIndex, "precinct_part");
                String ageGroup = getValue(row, colIndex, "age_group_2024");
                String gender = getValue(row, colIndex, "gender");
                String statusCode = getValue(row, colIndex, "status_code");

                if (stateAbbr == null || countyCode == null || precinctCode == null || 
                    ageGroup == null || gender == null || statusCode == null) {
                    skipped++;
                    continue;
                }

                // Use empty string if precinctPart is null
                String part = (precinctPart == null) ? "" : precinctPart;
                String docId = stateAbbr.toUpperCase() + "|" + countyCode + "|" + precinctCode + "|" + part + "|" + ageGroup + "|" + gender + "|" + statusCode;

                WaDemographicsPrecinctDoc demo = new WaDemographicsPrecinctDoc();
                demo.setId(docId);
                demo.setStateAbbr(stateAbbr.toUpperCase());
                demo.setCountyCode(countyCode);
                demo.setCountyName(getValue(row, colIndex, "county_name"));
                demo.setPrecinctCode(precinctCode);
                demo.setPrecinctPart(part);
                demo.setLegislativeDistrict(getValue(row, colIndex, "legislative_district"));
                demo.setCongressionalDistrict(getValue(row, colIndex, "congressional_district"));
                demo.setAgeGroup2024(ageGroup);
                demo.setGender(gender);
                demo.setStatusCode(statusCode);
                demo.setRegisteredVoters(parseInt(getValue(row, colIndex, "registered_voters")));

                repo.save(demo);
                processed++;

                if (processed % 100 == 0) {
                    System.out.println("[WaVrdbPrecinctDemoImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[WaVrdbPrecinctDemoImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[WaVrdbPrecinctDemoImporter] Import complete:");
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



