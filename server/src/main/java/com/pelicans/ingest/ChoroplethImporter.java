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
// If you want to run this only under a profile, uncomment and run with -Dspring-boot.run.profiles=ingest
// import org.springframework.context.annotation.Profile;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// @Profile("ingest")   // optional safety: enable if you want to avoid running in web profile
@Component
@ConditionalOnProperty(name = "eavs.choro.enabled", havingValue = "true", matchIfMissing = false)
public class ChoroplethImporter implements CommandLineRunner {

    private final ProvisionalChoroplethRepository repo;
    private final ObjectMapper mapper;

    @Value("${eavs.choro.input:provisional_chloropleth.json}")
    private String inputPath;

    @Value("${eavs.choro.year:2024}")
    private int year;

    @Value("${eavs.choro.measure:E1A}")
    private String measure;

    public ChoroplethImporter(ProvisionalChoroplethRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    // Valid 2-digit state FIPS (01..56, incl. DC=11)
    private static final Set<String> VALID_STATES = new HashSet<>(Arrays.asList(
            "01","02","04","05","06","08","09","10","11","12","13","15",
            "16","17","18","19","20","21","22","23","24","25","26","27",
            "28","29","30","31","32","33","34","35","36","37","38","39",
            "40","41","42","44","45","46","47","48","49","50","51","53",
            "54","55","56"
    ));

    @Override
    public void run(String... args) throws Exception {
        File f = new File(inputPath);
        if (!f.exists()) {
            System.out.println("[ChoroplethImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        JsonNode root = mapper.readTree(f);

        String regionCol = text(root, "region_col", "FIPSCODE");
        String valueCol  = text(root, "value_col", "E1A");

        JsonNode data = root.path("data");
        if (!data.isArray()) {
            System.out.println("[ChoroplethImporter] 'data' must be an array.");
            return;
        }

        int binsCount = root.path("bins").isArray() ? root.path("bins").size() : 0;

        ProvisionalChoroplethDoc doc = new ProvisionalChoroplethDoc();
        doc.setYear(year);
        doc.setMeasure(measure);
        doc.setBins(binsCount);
        doc.setId(year + "|" + measure);

        Map<String, ProvisionalChoroplethDoc.StateValues> states = new HashMap<>();

        int seen = 0, kept = 0, dropped = 0;

        for (JsonNode row : data) {
            seen++;

            JsonNode regionNode = row.get(regionCol);
            JsonNode valueNode  = row.get(valueCol);
            if (regionNode == null || valueNode == null || !valueNode.isNumber()) {
                dropped++;
                continue;
            }

            String countyRaw  = regionNode.asText();
            String countyFips = normalizeCountyFips(countyRaw); // -> 5 chars or null

            if (countyFips == null || countyFips.length() != 5) {
                dropped++;
                continue;
            }

            String stateFips = countyFips.substring(0, 2);
            if (!VALID_STATES.contains(stateFips)) {
                dropped++;
                continue;
            }

            // ⬇️ NEW: drop statewide/placeholder rows like 19000, 56000, etc.
            if (countyFips.endsWith("000")) {
                dropped++;
                continue;
            }

            double value = valueNode.asDouble();

            ProvisionalChoroplethDoc.StateValues sv =
                    states.computeIfAbsent(stateFips, k -> new ProvisionalChoroplethDoc.StateValues());

            sv.getValues().put(countyFips, value);
            kept++;
        }

        // ⬇️ NEW: remove states that ended up empty
        states.entrySet().removeIf(e -> e.getValue().getValues().isEmpty());

        doc.setStates(states);

        // save (upsert by id)
        repo.save(doc);

        System.out.println("[ChoroplethImporter] Saved choropleth: year=" + year
                + ", measure=" + measure
                + ", bins=" + binsCount
                + ", states=" + states.size()
                + ", rows_seen=" + seen
                + ", rows_kept=" + kept
                + ", rows_dropped=" + dropped);
    }

    /** Normalize raw county FIPS to a strict 5-digit code (strip non-digits, left-pad, truncate). */
    private static String normalizeCountyFips(String raw) {
        if (raw == null) return null;
        // remove all non-digits
        String s = raw.trim().replaceAll("\\D", "");
        if (s.isEmpty()) return null;

        // Left pad to 5
        if (s.length() < 5) {
            s = String.format("%5s", s).replace(' ', '0');
        }

        // Truncate to 5 if longer (guards against junk like "100100000")
        if (s.length() > 5) {
            s = s.substring(0, 5);
        }

        return s;
        // Optional stricter check:
        // if (!s.matches("^[0-9]{5}$")) return null;
        // return s;
    }

    private static String text(JsonNode n, String field, String def) {
        JsonNode v = n.get(field);
        return (v != null && v.isTextual()) ? v.asText() : def;
    }
}
