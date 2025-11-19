package com.pelicans.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pelicans.model.GeoCountyDoc;
import com.pelicans.model.GeoStateDoc;
import com.pelicans.repository.GeoCountyRepository;
import com.pelicans.repository.GeoStateRepository;
import org.bson.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/json")
public class JsonController {

    private final GeoStateRepository geoStateRepo;
    private final GeoCountyRepository geoCountyRepo;
    private final ObjectMapper objectMapper;

    public JsonController(GeoStateRepository geoStateRepo, GeoCountyRepository geoCountyRepo, ObjectMapper objectMapper) {
        this.geoStateRepo = geoStateRepo;
        this.geoCountyRepo = geoCountyRepo;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/us-states")
    public ResponseEntity<String> usStatesJson() {
        try {
            List<GeoStateDoc> states = geoStateRepo.findAll();
            
            ObjectNode featureCollection = objectMapper.createObjectNode();
            featureCollection.put("type", "FeatureCollection");
            
            ArrayNode features = objectMapper.createArrayNode();
            
            for (GeoStateDoc state : states) {
                ObjectNode feature = objectMapper.createObjectNode();
                feature.put("type", "Feature");
                
                // Properties that frontend expects
                ObjectNode properties = objectMapper.createObjectNode();
                properties.put("STATEFP", state.getStateFips());
                properties.put("NAME", state.getStateName());
                properties.put("stateAbbr", state.getStateAbbr());
                properties.put("stateName", state.getStateName());
                if (state.getCentroidLon() != null) {
                    properties.put("centroidLon", state.getCentroidLon());
                }
                if (state.getCentroidLat() != null) {
                    properties.put("centroidLat", state.getCentroidLat());
                }
                
                feature.set("properties", properties);
                
                // Convert BSON Document to JSON node for geometry
                if (state.getGeometry() != null) {
                    String geometryJson = state.getGeometry().toJson();
                    feature.set("geometry", objectMapper.readTree(geometryJson));
                }
                
                features.add(feature);
            }
            
            featureCollection.set("features", features);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(featureCollection));
                    
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Failed to generate GeoJSON: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/us-counties")
    public ResponseEntity<String> usCountiesJson() {
        try {
            List<GeoCountyDoc> counties = geoCountyRepo.findAll();
            List<GeoStateDoc> states = geoStateRepo.findAll();
            
            // Build a map of stateAbbr -> stateFips for efficient lookup
            Map<String, String> stateFipsMap = new HashMap<>();
            for (GeoStateDoc state : states) {
                stateFipsMap.put(state.getStateAbbr(), state.getStateFips());
            }
            
            ObjectNode featureCollection = objectMapper.createObjectNode();
            featureCollection.put("type", "FeatureCollection");
            
            ArrayNode features = objectMapper.createArrayNode();
            
            for (GeoCountyDoc county : counties) {
                ObjectNode feature = objectMapper.createObjectNode();
                feature.put("type", "Feature");
                
                // Properties that frontend expects
                ObjectNode properties = objectMapper.createObjectNode();
                // GEOID is the 5-digit FIPS code (frontend uses this for matching)
                properties.put("GEOID", county.getFips5());
                // STATEFP is the 2-digit state FIPS (frontend filters by this)
                String stateFips = stateFipsMap.get(county.getStateAbbr());
                if (stateFips != null) {
                    properties.put("STATEFP", stateFips);
                }
                properties.put("NAME", county.getCountyName());
                properties.put("countyName", county.getCountyName());
                properties.put("fips5", county.getFips5());
                properties.put("stateAbbr", county.getStateAbbr());
                if (county.getCentroidLon() != null) {
                    properties.put("centroidLon", county.getCentroidLon());
                }
                if (county.getCentroidLat() != null) {
                    properties.put("centroidLat", county.getCentroidLat());
                }
                
                feature.set("properties", properties);
                
                // Convert BSON Document to JSON node for geometry
                if (county.getGeometry() != null) {
                    String geometryJson = county.getGeometry().toJson();
                    feature.set("geometry", objectMapper.readTree(geometryJson));
                }
                
                features.add(feature);
            }
            
            featureCollection.set("features", features);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(featureCollection));
                    
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Failed to generate GeoJSON: " + e.getMessage() + "\"}");
        }
    }

}
