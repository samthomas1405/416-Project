package com.pelicans.controller;

import org.springframework.cache.annotation.Cacheable;
import com.pelicans.model.ProvisionalChoroplethDoc;
import com.pelicans.repository.ProvisionalChoroplethRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
     * Full doc - returns the entire document
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
        
        return ResponseEntity.ok(doc);
    }

    /**
     * State-only values (preferred for the frontend)
     * GET /api/choropleth/{year}/{measure}/state?state=25
     * Response: { "bins": <array>, "values": { "<countyFips>": <number>, ... } }
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

        // Get state values
        ProvisionalChoroplethDoc.StateValues stateValues = doc.getStates() != null 
            ? doc.getStates().get(stateFips) 
            : null;

        Map<String, Object> body = new HashMap<>();
        body.put("bins", doc.getBins());
        body.put("values", stateValues != null && stateValues.getValues() != null 
            ? stateValues.getValues() 
            : new HashMap<>());
        
        return ResponseEntity.ok(body);
    }
}
