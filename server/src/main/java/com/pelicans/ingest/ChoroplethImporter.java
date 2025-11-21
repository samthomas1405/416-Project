// src/main/java/com/pelicans/ingest/ChoroplethImporter.java
package com.pelicans.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelicans.model.ProvisionalChoroplethDoc;
import com.pelicans.repository.ProvisionalChoroplethRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "eavs.choro.enabled", havingValue = "true", matchIfMissing = false)
public class ChoroplethImporter implements CommandLineRunner {

    private final ProvisionalChoroplethRepository repo;
    private final ObjectMapper mapper;

    @Value("${eavs.choro.input:/Users/samuelthomas/Downloads/data_clean/results_2024}")
    private String inputPath;

    // Valid state FIPS codes (2-digit)
    private static final Set<String> VALID_STATES = new HashSet<>(Arrays.asList(
        "01", "02", "04", "05", "06", "08", "09", "10", "11", "12", "13", "15", "16", "17", "18", "19",
        "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35",
        "36", "37", "38", "39", "40", "41", "42", "44", "45", "46", "47", "48", "49", "50", "51", "53",
        "54", "55", "56"
    ));

    public ChoroplethImporter(ProvisionalChoroplethRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        File inputDir = new File(inputPath);
        
        // Check if input is a directory or a single file
        File[] files;
        if (inputDir.isDirectory()) {
            // Process all *_choropleth.json files in the directory
            files = inputDir.listFiles((dir, name) -> name.endsWith("_choropleth.json"));
            if (files == null || files.length == 0) {
                System.out.println("[ChoroplethImporter] No choropleth JSON files found in: " + inputDir.getAbsolutePath());
                return;
            }
            System.out.println("[ChoroplethImporter] Found " + files.length + " choropleth file(s) to process");
        } else if (inputDir.isFile() && inputDir.getName().endsWith("_choropleth.json")) {
            // Single file mode (backward compatibility)
            files = new File[]{inputDir};
        } else {
            System.out.println("[ChoroplethImporter] Input path is neither a directory nor a choropleth JSON file: " + inputDir.getAbsolutePath());
            return;
        }

        int totalProcessed = 0;
        int totalSkipped = 0;

        for (File f : files) {
            try {
                System.out.println("[ChoroplethImporter] Processing: " + f.getName());
                processChoroplethFile(f);
                totalProcessed++;
            } catch (Exception e) {
                System.err.println("[ChoroplethImporter] Error processing " + f.getName() + ": " + e.getMessage());
                e.printStackTrace();
                totalSkipped++;
            }
        }

        System.out.println("[ChoroplethImporter] Import complete: " + totalProcessed + " processed, " + totalSkipped + " skipped");
    }

    private void processChoroplethFile(File f) throws Exception {
        JsonNode root = mapper.readTree(f);

        // Extract metadata
        String valueCol = text(root, "value_col", "E1A");
        int year = extractYear(root);
        String measure = valueCol;

        // Extract bins
        List<Double> bins = new ArrayList<>();
        JsonNode binsNode = root.path("bins");
        if (binsNode.isArray()) {
            for (JsonNode bin : binsNode) {
                if (bin.isNumber()) {
                    bins.add(bin.asDouble());
                }
            }
        }

        // Extract data and organize by state
        Map<String, ProvisionalChoroplethDoc.StateValues> statesMap = new HashMap<>();
        JsonNode data = root.path("data");
        String regionCol = text(root, "region_col", "FIPSCODE");

        if (data.isArray()) {
            for (JsonNode row : data) {
                JsonNode regionNode = row.path(regionCol);
                if (regionNode.isMissingNode() || regionNode.isNull()) continue;

                String countyRaw = regionNode.asText().trim();
                String countyFips = normalizeCountyFips(countyRaw);
                if (countyFips == null || countyFips.length() != 5) continue;

                // Extract state FIPS (first 2 digits)
                String stateFips = countyFips.substring(0, 2);
                if (!VALID_STATES.contains(stateFips)) continue;

                // Skip state-level aggregations (ending in 000)
                if (countyFips.endsWith("000")) continue;

                // Get the value
                JsonNode valueNode = row.path(valueCol);
                if (!valueNode.isNumber()) continue;
                double value = valueNode.asDouble();

                // Add to state map
                statesMap.computeIfAbsent(stateFips, k -> {
                    ProvisionalChoroplethDoc.StateValues sv = new ProvisionalChoroplethDoc.StateValues();
                    sv.setValues(new HashMap<>());
                    return sv;
                }).getValues().put(countyFips, value);
            }
        }

        // Create and save document
        ProvisionalChoroplethDoc doc = new ProvisionalChoroplethDoc();
        doc.setId(year + "|" + measure);
        doc.setYear(year);
        doc.setMeasure(measure);
        doc.setBins(bins);
        doc.setStates(statesMap);

        repo.save(doc);

        System.out.println("[ChoroplethImporter] Saved choropleth: year=" + year
                + ", measure=" + measure
                + ", bins=" + bins.size()
                + ", states=" + statesMap.size()
                + ", file=" + f.getName());
    }

    /**
     * Normalize raw county FIPS to a strict 5-digit code.
     */
    private static String normalizeCountyFips(String raw) {
        if (raw == null) return null;
        // Remove all non-digits
        String s = raw.trim().replaceAll("\\D", "");
        if (s.isEmpty()) return null;

        // Left pad to 5
        if (s.length() < 5) {
            s = String.format("%5s", s).replace(' ', '0');
        }

        // Truncate to 5 if longer
        if (s.length() > 5) {
            s = s.substring(0, 5);
        }

        return s;
    }

    private int extractYear(JsonNode root) {
        // Try to extract year from meta.source (e.g., "data/raw/eavs/2024/eavs_2024.csv")
        JsonNode meta = root.path("meta");
        if (meta.has("source")) {
            String source = meta.get("source").asText();
            // Look for 4-digit year pattern
            Pattern yearPattern = Pattern.compile("\\b(20\\d{2})\\b");
            java.util.regex.Matcher matcher = yearPattern.matcher(source);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    // Fall through to default
                }
            }
        }
        // Default to 2024 if not found
        return 2024;
    }

    private static String text(JsonNode n, String field, String def) {
        JsonNode v = n.get(field);
        return (v != null && v.isTextual()) ? v.asText() : def;
    }
}
