package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.EquipmentDeviceDoc;
import com.pelicans.repository.EquipmentDeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "equipment.device.enabled", havingValue = "true", matchIfMissing = false)
public class EquipmentDevice2022Importer implements CommandLineRunner {

    private final EquipmentDeviceRepository repo;

    @Value("${equipment.device.input:data_clean/equipment/equipment_2022_with_quality.csv}")
    private String inputPath;

    public EquipmentDevice2022Importer(EquipmentDeviceRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[EquipmentDevice2022Importer] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[EquipmentDevice2022Importer] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[EquipmentDevice2022Importer] CSV is empty");
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
                String manufacturer = getValue(row, colIndex, "manufacturer");
                String model = getValue(row, colIndex, "model");
                String equipmentType = getValue(row, colIndex, "equipment type");

                if (stateAbbr == null || fipsCode == null || manufacturer == null || model == null || equipmentType == null) {
                    skipped++;
                    continue;
                }

                String docId = stateAbbr.toUpperCase() + "|" + fipsCode + "|" + manufacturer + "|" + model + "|" + equipmentType;

                EquipmentDeviceDoc equipment = new EquipmentDeviceDoc();
                equipment.setId(docId);
                equipment.setStateAbbr(stateAbbr.toUpperCase());
                equipment.setFipsCode(fipsCode);
                equipment.setStateName(getValue(row, colIndex, "state"));
                equipment.setJurisdiction(getValue(row, colIndex, "jurisdiction"));
                equipment.setEquipmentType(equipmentType);
                equipment.setManufacturer(manufacturer);
                equipment.setModel(model);
                equipment.setFirstYearInUse(parseInt(getValue(row, colIndex, "first year in use")));
                equipment.setBarcode(getValue(row, colIndex, "barcode"));
                equipment.setVppat(getValue(row, colIndex, "vvpat"));
                equipment.setElectionDayStandard(parseBoolean(getValue(row, colIndex, "election day standard")));
                equipment.setElectionDayAccessible(parseBoolean(getValue(row, colIndex, "election day accessible")));
                equipment.setEarlyVotingStandard(parseBoolean(getValue(row, colIndex, "early voting standard")));
                equipment.setEarlyVotingAccessible(parseBoolean(getValue(row, colIndex, "early voting accessible")));
                equipment.setMailBallotEquipment(parseBoolean(getValue(row, colIndex, "mail ballot equipment")));
                equipment.setExtraText(getValue(row, colIndex, "extra text"));
                equipment.setQualityScore(parseDouble(getValue(row, colIndex, "quality_score")));

                repo.save(equipment);
                processed++;

                if (processed % 50 == 0) {
                    System.out.println("[EquipmentDevice2022Importer] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[EquipmentDevice2022Importer] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[EquipmentDevice2022Importer] Import complete:");
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

    private Boolean parseBoolean(String s) {
        if (s == null || s.isEmpty()) return null;
        String lower = s.toLowerCase().trim();
        return lower.equals("true") || lower.equals("yes") || lower.equals("1");
    }

    private Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }
        return colIndex;
    }
}



