// src/main/java/com/pelicans/ingest/ChoroplethImporter.java
package com.pelicans.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelicans.model.ProvisionalChoroplethDoc;
import com.pelicans.repository.ProvisionalChoroplethRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "eavs.choro.enabled", havingValue = "true", matchIfMissing = false)
public class ChoroplethImporter implements CommandLineRunner {

    private final ProvisionalChoroplethRepository repo;
    private final ObjectMapper mapper;

    @Value("${eavs.choro.input:/Users/samuelthomas/Downloads/data_clean/results_2024}")
    private String inputPath;

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
        // Read the entire JSON file
        JsonNode root = mapper.readTree(f);

        // Extract metadata for indexing/querying
        String valueCol = text(root, "value_col", "E1A");
        int year = extractYear(root);
        String measure = valueCol;
        
        // Convert JsonNode to MongoDB Document (stores raw JSON)
        Document rawDataDoc = jsonNodeToDocument(root);

        // Create document with raw JSON
        ProvisionalChoroplethDoc doc = new ProvisionalChoroplethDoc();
        doc.setId(year + "|" + measure);
        doc.setYear(year);
        doc.setMeasure(measure);
        doc.setRawData(rawDataDoc);

        // Save (upsert by id)
        repo.save(doc);

        // Get some stats for logging
        JsonNode data = root.path("data");
        int dataRows = data.isArray() ? data.size() : 0;
        JsonNode bins = root.path("bins");
        int binsCount = bins.isArray() ? bins.size() : 0;

        System.out.println("[ChoroplethImporter] Saved choropleth: year=" + year
                + ", measure=" + measure
                + ", bins=" + binsCount
                + ", data_rows=" + dataRows
                + ", file=" + f.getName());
    }

    /**
     * Converts a Jackson JsonNode to a MongoDB Document recursively.
     * This preserves the entire JSON structure as-is.
     */
    private Document jsonNodeToDocument(JsonNode node) {
        Document doc = new Document();
        
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                doc.append(key, jsonNodeValueToObject(value));
            });
        } else if (node.isArray()) {
            // This shouldn't happen at root level, but handle it
            return new Document("_array", jsonNodeValueToObject(node));
        }
        
        return doc;
    }

    /**
     * Converts a JsonNode value to a Java object that MongoDB can store.
     */
    private Object jsonNodeValueToObject(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isLong()) {
            return node.asLong();
        } else if (node.isDouble() || node.isFloat()) {
            return node.asDouble();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isArray()) {
            return node.elements().hasNext() 
                ? java.util.stream.StreamSupport.stream(node.spliterator(), false)
                    .map(this::jsonNodeValueToObject)
                    .collect(java.util.stream.Collectors.toList())
                : new java.util.ArrayList<>();
        } else if (node.isObject()) {
            Document subDoc = new Document();
            node.fields().forEachRemaining(entry -> {
                subDoc.append(entry.getKey(), jsonNodeValueToObject(entry.getValue()));
            });
            return subDoc;
        } else {
            // Fallback: convert to string
            return node.asText();
        }
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
