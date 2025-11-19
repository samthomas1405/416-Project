package com.pelicans.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelicans.model.GeoStateDoc;
import com.pelicans.repository.GeoStateRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "geo.states.enabled", havingValue = "true", matchIfMissing = false)
public class GeoStatesImporter implements CommandLineRunner {

    private final GeoStateRepository repo;
    private final ObjectMapper mapper;

    @Value("${geo.states.input:data_clean/geo/us_states.geojson}")
    private String inputPath;

    public GeoStatesImporter(GeoStateRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        File f = new File(inputPath);
        if (!f.exists()) {
            System.out.println("[GeoStatesImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        JsonNode root = mapper.readTree(f);
        JsonNode features = root.path("features");
        if (!features.isArray()) {
            System.out.println("[GeoStatesImporter] Invalid GeoJSON (features missing)");
            return;
        }

        int processed = 0;
        int skipped = 0;

        for (JsonNode feature : features) {
            try {
                JsonNode props = feature.path("properties");
                String stateAbbr = text(props, "state_abbr");
                String stateName = text(props, "state_name");
                String stateFips = text(props, "state_fips");
                if (stateFips != null && stateFips.startsWith("US") && stateFips.length() > 2) {
                    stateFips = stateFips.substring(2);
                }

                if (stateAbbr == null || stateName == null || stateFips == null) {
                    skipped++;
                    continue;
                }

                GeoStateDoc state = new GeoStateDoc();
                state.setId(stateAbbr.toUpperCase(Locale.US));
                state.setStateAbbr(stateAbbr.toUpperCase(Locale.US));
                state.setStateName(stateName);
                state.setStateFips(stateFips);
                state.setCentroidLon(doubleValue(props, "centroid_lon"));
                state.setCentroidLat(doubleValue(props, "centroid_lat"));
                state.setGeometry(geometry(feature));

                repo.save(state);
                processed++;

                if (processed % 10 == 0) {
                    System.out.println("[GeoStatesImporter] Processed: " + processed);
                }
            } catch (Exception ex) {
                skipped++;
                if (skipped <= 5) {
                    System.err.println("[GeoStatesImporter] Error: " + ex.getMessage());
                }
            }
        }

        System.out.println("[GeoStatesImporter] Import complete:");
        System.out.println("  Processed: " + processed);
        System.out.println("  Skipped: " + skipped);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asText();
        return text == null || text.isEmpty() ? null : text.trim();
    }

    private Double doubleValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (value.isNumber()) return value.asDouble();
        String text = value.asText();
        if (text == null || text.isEmpty()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Document geometry(JsonNode feature) {
        JsonNode geomNode = feature.path("geometry");
        if (geomNode.isMissingNode() || geomNode.isNull()) {
            return null;
        }
        return Document.parse(geomNode.toString());
    }
}

