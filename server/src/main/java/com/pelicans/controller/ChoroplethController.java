package com.pelicans.controller;

import org.bson.Document;
import org.springframework.cache.annotation.Cacheable;
import com.pelicans.model.ProvisionalChoroplethDoc;
import com.pelicans.repository.ProvisionalChoroplethRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/choropleth")
public class ChoroplethController {

    private final ProvisionalChoroplethRepository repo;

    public ChoroplethController(ProvisionalChoroplethRepository repo) {
        this.repo = repo;
    }

    /**
     * Full doc - returns the entire raw JSON
     * GET /api/choropleth/{year}/{measure}
     */
    @Cacheable
    @GetMapping("/{year}/{measure}")
    public ResponseEntity<?> getFullDoc(
            @PathVariable int year,
            @PathVariable String measure
    ) {
        String id = year + "|" + measure;
        ProvisionalChoroplethDoc doc = repo.findById(id).orElse(null);
        if (doc == null) return ResponseEntity.notFound().build();
        
        // Return the raw data document
        return ResponseEntity.ok(doc.getRawData());
    }

    /**
     * State-only values (preferred for the frontend)
     * GET /api/choropleth/{year}/{measure}/state?state=25
     * Response: { "bins": <int>, "values": { "<countyFips>": <number>, ... } }
     */
    @Cacheable
    @GetMapping("/{year}/{measure}/state")
    public ResponseEntity<?> getStateSlice(
            @PathVariable int year,
            @PathVariable String measure,
            @RequestParam("state") String stateFips
    ) {
        String id = year + "|" + measure;
        ProvisionalChoroplethDoc doc = repo.findById(id).orElse(null);
        if (doc == null) return ResponseEntity.notFound().build();

        Document rawData = doc.getRawData();
        if (rawData == null) {
            return ResponseEntity.notFound().build();
        }

        // Extract bins count
        List<?> bins = rawData.getList("bins", Object.class);
        int binsCount = bins != null ? bins.size() : 0;

        // Extract region_col and value_col
        String regionCol = rawData.getString("region_col");
        String valueCol = rawData.getString("value_col");
        if (regionCol == null || valueCol == null) {
            return ResponseEntity.badRequest().build();
        }

        // Filter data array for the requested state
        List<Document> data = rawData.getList("data", Document.class);
        Map<String, Object> values = new HashMap<>();
        
        if (data != null) {
            for (Document row : data) {
                Object regionObj = row.get(regionCol);
                if (regionObj == null) continue;
                
                String countyRaw = regionObj.toString().trim();
                // Normalize to 5-digit FIPS
                String countyFips = normalizeCountyFips(countyRaw);
                if (countyFips == null || countyFips.length() != 5) continue;
                
                // Check if this county belongs to the requested state
                String rowStateFips = countyFips.substring(0, 2);
                if (!rowStateFips.equals(stateFips)) continue;
                
                // Skip state-level aggregations
                if (countyFips.endsWith("000")) continue;
                
                // Get the value
                Object value = row.get(valueCol);
                if (value != null) {
                    values.put(countyFips, value);
                }
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("bins", binsCount);
        body.put("values", values);
        return ResponseEntity.ok(body);
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
}
