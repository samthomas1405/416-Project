package com.pelicans.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelicans.model.GeoCountyDoc;
import com.pelicans.repository.GeoCountyRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "geo.counties.enabled", havingValue = "true", matchIfMissing = false)
public class GeoCountiesImporter implements CommandLineRunner {

    private final GeoCountyRepository repo;
    private final ObjectMapper mapper;

    @Value("${geo.counties.input:data_clean/geo/us_counties_selected.geojson}")
    private String inputPath;

    public GeoCountiesImporter(GeoCountyRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        File f = new File(inputPath);
        if (!f.exists()) {
            System.out.println("[GeoCountiesImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        JsonNode root = mapper.readTree(f);
        JsonNode features = root.path("features");
        if (!features.isArray()) {
            System.out.println("[GeoCountiesImporter] Invalid GeoJSON (features missing)");
            return;
        }

        int processed = 0;
        int skipped = 0;

        for (JsonNode feature : features) {
            try {
                JsonNode props = feature.path("properties");
                String stateAbbr = text(props, "state_abbr");
                String fips5 = text(props, "fips5");
                String countyName = text(props, "county_name");

                if (stateAbbr == null || fips5 == null || countyName == null) {
                    skipped++;
                    continue;
                }

                GeoCountyDoc county = new GeoCountyDoc();
                String id = stateAbbr.toUpperCase(Locale.US) + "|" + fips5;
                county.setId(id);
                county.setStateAbbr(stateAbbr.toUpperCase(Locale.US));
                county.setFips5(fips5);
                county.setCountyName(countyName);
                county.setCentroidLon(doubleValue(props, "centroid_lon"));
                county.setCentroidLat(doubleValue(props, "centroid_lat"));
                county.setGeometry(geometry(feature));

                repo.save(county);
                processed++;

                if (processed % 25 == 0) {
                    System.out.println("[GeoCountiesImporter] Processed: " + processed);
                }
            } catch (Exception ex) {
                skipped++;
                if (skipped <= 5) {
                    System.err.println("[GeoCountiesImporter] Error: " + ex.getMessage());
                }
            }
        }

        System.out.println("[GeoCountiesImporter] Import complete:");
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

