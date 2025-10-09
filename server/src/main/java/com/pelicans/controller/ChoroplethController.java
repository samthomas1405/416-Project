package com.pelicans.controller;

import com.pelicans.model.ProvisionalChoroplethDoc;
import com.pelicans.repo.ProvisionalChoroplethRepository;
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
     * Full doc (rarely used now)
     * GET /api/choropleth/{year}/{measure}
     */
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
     * Response: { "bins": <int>, "values": { "<countyFips>": <number>, ... } }
     */
    @GetMapping("/{year}/{measure}/state")
    public ResponseEntity<?> getStateSlice(
            @PathVariable int year,
            @PathVariable String measure,
            @RequestParam("state") String stateFips
    ) {
        String id = year + "|" + measure;
        ProvisionalChoroplethDoc doc = repo.findById(id).orElse(null);
        if (doc == null) return ResponseEntity.notFound().build();

        ProvisionalChoroplethDoc.StateValues sv = doc.getStates().get(stateFips);
        Map<String, Object> body = new HashMap<>();
        body.put("bins", doc.getBins());
        body.put("values", sv != null ? sv.getValues() : new HashMap<>());
        return ResponseEntity.ok(body);
    }
}
