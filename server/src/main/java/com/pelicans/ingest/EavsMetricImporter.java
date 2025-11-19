package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.EavsMetricDoc;
import com.pelicans.repository.EavsMetricRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "eavs.csv.enabled", havingValue = "true", matchIfMissing = false)
public class EavsMetricImporter implements CommandLineRunner {

    private final EavsMetricRepository repo;

    @Value("${eavs.csv.input:data_clean/eavs/eavs_2024_cleaned.csv}")
    private String inputPath;

    @Value("${eavs.csv.year:2024}")
    private int year;

    public EavsMetricImporter(EavsMetricRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[EavsMetricImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[EavsMetricImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[EavsMetricImporter] CSV is empty");
            return;
        }

        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toUpperCase(), i);
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
                String fipsCode = getValue(row, colIndex, "FIPSCODE");
                if (fipsCode == null || fipsCode.isEmpty()) {
                    skipped++;
                    continue;
                }

                // Skip state-level aggregations (FIPS ending in 000)
                if (fipsCode.endsWith("000") && fipsCode.length() >= 5) {
                    skipped++;
                    continue;
                }

                // Extract state FIPS (first 2 digits)
                String stateFips = fipsCode.length() >= 2 ? fipsCode.substring(0, 2) : null;
                if (stateFips == null) {
                    skipped++;
                    continue;
                }

                // Create region ID: stateFips-countyFips (e.g., "01-001")
                String regionId = stateFips + "-" + (fipsCode.length() >= 5 ? fipsCode.substring(2) : fipsCode);
                String docId = year + "|" + stateFips + "|" + regionId;

                EavsMetricDoc metric = new EavsMetricDoc();
                metric.setId(docId);
                metric.setYear(year);
                metric.setRegionId(regionId);
                metric.setStateId(stateFips);

                // Build categories
                EavsMetricDoc.Categories categories = new EavsMetricDoc.Categories();
                
                // Provisional ballots (E1*, E2*)
                Map<String, Object> provisional = buildProvisionalMap(row, colIndex);
                categories.setProvisional(provisional);

                // Active/Inactive voters (A1A, A1B, A1C)
                Map<String, Object> activeVoters = buildActiveVotersMap(row, colIndex);
                categories.setActiveVoters(activeVoters);

                // Pollbook deletions (A12*)
                Map<String, Object> pollbookDeletions = buildPollbookDeletionsMap(row, colIndex);
                categories.setPollbookDeletions(pollbookDeletions);

                // Mail rejections (C9* or D*)
                Map<String, Object> mailRejections = buildMailRejectionsMap(row, colIndex);
                categories.setMailRejections(mailRejections);

                // Early voting
                EavsMetricDoc.EarlyVoting earlyVoting = buildEarlyVoting(row, colIndex);
                categories.setEarlyVoting(earlyVoting);

                // Provisional rejected (E1D)
                EavsMetricDoc.Tot provisionalRejected = new EavsMetricDoc.Tot();
                Double e1d = getNumericValue(row, colIndex, "E1D");
                provisionalRejected.setTotal(e1d);
                categories.setProvisionalRejected(provisionalRejected);

                metric.setCategories(categories);

                // Derived metrics
                Map<String, Object> derived = new HashMap<>();
                Double missingnessScore = getNumericValue(row, colIndex, "MISSINGNESS_SCORE");
                if (missingnessScore != null) {
                    derived.put("missingnessScore", missingnessScore);
                }
                Double equipmentQualityScore = getNumericValue(row, colIndex, "EQUIPMENT_QUALITY_SCORE");
                if (equipmentQualityScore != null) {
                    derived.put("equipmentQualityScore", equipmentQualityScore);
                }
                metric.setDerived(derived);

                // Provenance
                Map<String, Object> provenance = new HashMap<>();
                provenance.put("source", "EAVS 2024 CSV");
                provenance.put("version", "1.0");
                provenance.put("ingestedAt", new Date());
                provenance.put("filePath", inputPath);
                metric.setProvenance(provenance);

                Date now = new Date();
                metric.setCreatedAt(now);
                metric.setUpdatedAt(now);

                repo.save(metric);
                processed++;

                if (processed % 100 == 0) {
                    System.out.println("[EavsMetricImporter] Processed " + processed + " records...");
                }

            } catch (Exception e) {
                errors++;
                System.err.println("[EavsMetricImporter] Error processing row " + i + ": " + e.getMessage());
                if (errors > 10) {
                    System.err.println("[EavsMetricImporter] Too many errors, stopping");
                    break;
                }
            }
        }

        System.out.println("[EavsMetricImporter] Import complete:");
        System.out.println("  Processed: " + processed);
        System.out.println("  Skipped: " + skipped);
        System.out.println("  Errors: " + errors);
    }

    private Map<String, Object> buildProvisionalMap(String[] row, Map<String, Integer> colIndex) {
        Map<String, Object> provisional = new HashMap<>();
        
        // E1 series (totals)
        putIfPresent(provisional, "E1A", getNumericValue(row, colIndex, "E1A"));
        putIfPresent(provisional, "E1B", getNumericValue(row, colIndex, "E1B"));
        putIfPresent(provisional, "E1C", getNumericValue(row, colIndex, "E1C"));
        putIfPresent(provisional, "E1D", getNumericValue(row, colIndex, "E1D"));
        putIfPresent(provisional, "E1E", getNumericValue(row, colIndex, "E1E"));

        // E2 series (reasons) - these are what the frontend uses for the bar chart
        putIfPresent(provisional, "E2A", getNumericValue(row, colIndex, "E2A"));
        putIfPresent(provisional, "E2B", getNumericValue(row, colIndex, "E2B"));
        putIfPresent(provisional, "E2C", getNumericValue(row, colIndex, "E2C"));
        putIfPresent(provisional, "E2D", getNumericValue(row, colIndex, "E2D"));
        putIfPresent(provisional, "E2E", getNumericValue(row, colIndex, "E2E"));
        putIfPresent(provisional, "E2F", getNumericValue(row, colIndex, "E2F"));
        putIfPresent(provisional, "E2G", getNumericValue(row, colIndex, "E2G"));
        putIfPresent(provisional, "E2H", getNumericValue(row, colIndex, "E2H"));
        putIfPresent(provisional, "E2I", getNumericValue(row, colIndex, "E2I"));
        
        // E2J, E2K, E2L (other categories)
        putIfPresent(provisional, "E2J", getNumericValue(row, colIndex, "E2J"));
        putIfPresent(provisional, "E2K", getNumericValue(row, colIndex, "E2K"));
        putIfPresent(provisional, "E2L", getNumericValue(row, colIndex, "E2L"));

        return provisional;
    }

    private Map<String, Object> buildActiveVotersMap(String[] row, Map<String, Integer> colIndex) {
        Map<String, Object> activeVoters = new HashMap<>();
        
        // A1A = Active voters, A1B = Inactive voters, A1C = Total
        Double active = getNumericValue(row, colIndex, "A1A");
        Double inactive = getNumericValue(row, colIndex, "A1B");
        Double total = getNumericValue(row, colIndex, "A1C");
        
        if (active != null) activeVoters.put("active", active);
        if (inactive != null) activeVoters.put("inactive", inactive);
        if (total != null) activeVoters.put("total", total);

        return activeVoters;
    }

    private Map<String, Object> buildPollbookDeletionsMap(String[] row, Map<String, Integer> colIndex) {
        Map<String, Object> deletions = new HashMap<>();
        
        // A12 series
        for (String col : Arrays.asList("A12A", "A12B", "A12C", "A12D", "A12E", "A12F", "A12G", "A12H", "A12I")) {
            putIfPresent(deletions, col, getNumericValue(row, colIndex, col));
        }

        return deletions;
    }

    private Map<String, Object> buildMailRejectionsMap(String[] row, Map<String, Integer> colIndex) {
        Map<String, Object> rejections = new HashMap<>();
        
        // Try C9 series first (C9A, C9B, etc.)
        boolean hasC9 = false;
        for (char c = 'A'; c <= 'T'; c++) {
            String col = "C9" + c;
            Double val = getNumericValue(row, colIndex, col);
            if (val != null) {
                putIfPresent(rejections, col, val);
                hasC9 = true;
            }
        }

        // If no C9, try D series (D1, D2, etc.)
        if (!hasC9) {
            for (int i = 1; i <= 9; i++) {
                String col = "D" + i;
                putIfPresent(rejections, col, getNumericValue(row, colIndex, col));
            }
        }

        return rejections;
    }

    private EavsMetricDoc.EarlyVoting buildEarlyVoting(String[] row, Map<String, Integer> colIndex) {
        EavsMetricDoc.EarlyVoting earlyVoting = new EavsMetricDoc.EarlyVoting();
        
        // Try to find early voting totals
        // This may vary by state - common fields might be in B or C sections
        // For now, we'll store raw data and let the frontend calculate
        
        Map<String, Object> raw = new HashMap<>();
        
        // Look for common early voting indicators
        // B series often contains early voting data
        for (int i = 1; i <= 30; i++) {
            String col = "B" + i + "A";
            Double val = getNumericValue(row, colIndex, col);
            if (val != null) {
                raw.put(col, val);
            }
        }

        earlyVoting.setRaw(raw);
        
        // If we can identify specific fields, set them
        // This would need to be customized based on actual EAVS structure
        
        return earlyVoting;
    }

    private String getValue(String[] row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName.toUpperCase());
        if (idx == null || idx >= row.length) return null;
        String val = row[idx];
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    private Double getNumericValue(String[] row, Map<String, Integer> colIndex, String colName) {
        String val = getValue(row, colIndex, colName);
        if (val == null) return null;
        try {
            double d = Double.parseDouble(val);
            // Skip sentinel values
            if (d == -88.0 || d == -99.0 || d < 0) return null;
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void putIfPresent(Map<String, Object> map, String key, Double value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}

