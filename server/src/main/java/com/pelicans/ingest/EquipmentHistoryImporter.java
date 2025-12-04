package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.pelicans.model.EquipmentHistoryDoc;
import com.pelicans.repository.EquipmentHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.util.*;

@Component
@ConditionalOnProperty(name = "equipment.history.enabled", havingValue = "true", matchIfMissing = false)
public class EquipmentHistoryImporter implements CommandLineRunner {

    private final EquipmentHistoryRepository repo;

    @Value("${equipment.history.input:data_clean/gui/equipment_history_state_year_category.csv}")
    private String inputPath;

    public EquipmentHistoryImporter(EquipmentHistoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[EquipmentHistoryImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[EquipmentHistoryImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[EquipmentHistoryImporter] CSV is empty");
            return;
        }

        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = buildColumnIndex(headers);

        int processed = 0;
        int skipped = 0;
        int errors = 0;

        List<EquipmentHistoryDoc> batch = new ArrayList<>();
        final int BATCH_SIZE = 500;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < headers.length) {
                skipped++;
                continue;
            }

            try {
                String stateAbbr = getValue(row, colIndex, "state_abbr");
                String yearStr = getValue(row, colIndex, "year");
                String equipmentCategory = getValue(row, colIndex, "equipment_category");
                String deviceCountStr = getValue(row, colIndex, "device_count");

                if (stateAbbr == null || yearStr == null || equipmentCategory == null || deviceCountStr == null) {
                    skipped++;
                    continue;
                }

                Integer year = parseInt(yearStr);
                Integer deviceCount = parseInt(deviceCountStr);

                if (year == null || deviceCount == null) {
                    skipped++;
                    continue;
                }

                // Build composite ID: stateAbbr|year|equipmentCategory
                String docId = stateAbbr.toUpperCase() + "|" + year + "|" + equipmentCategory;

                EquipmentHistoryDoc doc = new EquipmentHistoryDoc();
                doc.setId(docId);
                doc.setStateAbbr(stateAbbr.toUpperCase());
                doc.setYear(year);
                doc.setEquipmentCategory(equipmentCategory);
                doc.setDeviceCount(deviceCount);

                batch.add(doc);

                if (batch.size() >= BATCH_SIZE) {
                    repo.saveAll(batch);
                    processed += batch.size();
                    batch.clear();
                    System.out.println("[EquipmentHistoryImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[EquipmentHistoryImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        // Save remaining batch
        if (!batch.isEmpty()) {
            repo.saveAll(batch);
            processed += batch.size();
        }

        System.out.println("[EquipmentHistoryImporter] Import complete:");
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

