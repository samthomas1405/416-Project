package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.FelonyPolicyDoc;
import com.pelicans.repository.FelonyPolicyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "felony.policy.enabled", havingValue = "true", matchIfMissing = false)
public class FelonyPolicy2024Importer implements CommandLineRunner {

    private final FelonyPolicyRepository repo;

    @Value("${felony.policy.input:data_clean/eavs/felony_policy_2024_q51.csv}")
    private String inputPath;

    public FelonyPolicy2024Importer(FelonyPolicyRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[FelonyPolicy2024Importer] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[FelonyPolicy2024Importer] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[FelonyPolicy2024Importer] CSV is empty");
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
                String stateAbbr = getValue(row, colIndex, "state");
                if (stateAbbr == null || stateAbbr.isEmpty()) {
                    skipped++;
                    continue;
                }

                String docId = stateAbbr.toUpperCase();

                FelonyPolicyDoc policy = new FelonyPolicyDoc();
                policy.setId(docId);
                policy.setStateAbbr(stateAbbr.toUpperCase());
                policy.setStateFull(getValue(row, colIndex, "state_full"));

                // Store all Q51 fields in a map
                Map<String, String> q51Fields = new HashMap<>();
                for (String header : headers) {
                    String upperHeader = header.toUpperCase();
                    if (upperHeader.startsWith("Q51") || upperHeader.startsWith("Q51A") || 
                        upperHeader.startsWith("Q51B") || upperHeader.startsWith("Q51C")) {
                        String value = getValue(row, colIndex, header);
                        if (value != null && !value.isEmpty()) {
                            q51Fields.put(header, value);
                        }
                    }
                }
                policy.setQ51Fields(q51Fields);

                repo.save(policy);
                processed++;

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[FelonyPolicy2024Importer] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[FelonyPolicy2024Importer] Import complete:");
        System.out.println("  Processed: " + processed);
        System.out.println("  Skipped: " + skipped);
        System.out.println("  Errors: " + errors);
    }

    private String getValue(String[] row, Map<String, Integer> colIndex, String colName) {
        // Column index uses lowercase keys, so convert colName to lowercase for lookup
        Integer idx = colIndex.get(colName.toLowerCase());
        if (idx == null || idx >= row.length) return null;
        String val = row[idx];
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    private Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }
        return colIndex;
    }
}

