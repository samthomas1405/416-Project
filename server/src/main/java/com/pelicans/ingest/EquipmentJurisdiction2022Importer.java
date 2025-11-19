package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.EquipmentJurisdictionDoc;
import com.pelicans.repository.EquipmentJurisdictionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "equipment.jurisdiction.enabled", havingValue = "true", matchIfMissing = false)
public class EquipmentJurisdiction2022Importer implements CommandLineRunner {

    private final EquipmentJurisdictionRepository repo;

    @Value("${equipment.jurisdiction.input:data_clean/equipment/equipment_quality_by_jurisdiction_2022.csv}")
    private String inputPath;

    public EquipmentJurisdiction2022Importer(EquipmentJurisdictionRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[EquipmentJurisdiction2022Importer] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[EquipmentJurisdiction2022Importer] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[EquipmentJurisdiction2022Importer] CSV is empty");
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
                String fipsCode = getValue(row, colIndex, "fips code");
                if (fipsCode == null) {
                    fipsCode = getValue(row, colIndex, "fips_code");  // Try underscore version
                }

                if (stateAbbr == null || fipsCode == null) {
                    skipped++;
                    continue;
                }

                String docId = stateAbbr.toUpperCase() + "|" + fipsCode;

                EquipmentJurisdictionDoc equipment = new EquipmentJurisdictionDoc();
                equipment.setId(docId);
                equipment.setStateAbbr(stateAbbr.toUpperCase());
                equipment.setFipsCode(fipsCode);
                equipment.setStateName(getValue(row, colIndex, "state_name"));
                equipment.setJurisdiction(getValue(row, colIndex, "jurisdiction"));
                equipment.setAvgQualityScore(parseDouble(getValue(row, colIndex, "avg_quality_score")));

                repo.save(equipment);
                processed++;

                if (processed % 50 == 0) {
                    System.out.println("[EquipmentJurisdiction2022Importer] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[EquipmentJurisdiction2022Importer] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[EquipmentJurisdiction2022Importer] Import complete:");
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

    private Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
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

