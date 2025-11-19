package com.pelicans.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelicans.model.GeoWaPrecinctDoc;
import com.pelicans.repository.GeoWaPrecinctRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "geo.wa.precincts.enabled", havingValue = "true", matchIfMissing = false)
public class GeoWaPrecinctsImporter implements CommandLineRunner {

    private final GeoWaPrecinctRepository repo;
    private final ObjectMapper mapper;

    @Value("${geo.wa.precincts.input:data_clean/geo/wa_precincts_2024.geojson}")
    private String inputPath;

    public GeoWaPrecinctsImporter(GeoWaPrecinctRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        File f = new File(inputPath);
        if (!f.exists()) {
            System.out.println("[GeoWaPrecinctsImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        JsonNode root = mapper.readTree(f);
        JsonNode features = root.path("features");
        if (!features.isArray()) {
            System.out.println("[GeoWaPrecinctsImporter] Invalid GeoJSON (features missing)");
            return;
        }

        int processed = 0;
        int skipped = 0;

        for (JsonNode feature : features) {
            try {
                JsonNode props = feature.path("properties");
                String stateAbbr = text(props, "state_abbr");
                String countyFips = text(props, "county_fips");
                String countyName = text(props, "county_name");
                String precinctNumber = text(props, "precinct_number");
                String precinctName = text(props, "precinct_name");

                if (stateAbbr == null || countyFips == null || precinctNumber == null || precinctName == null) {
                    skipped++;
                    continue;
                }

                GeoWaPrecinctDoc precinct = new GeoWaPrecinctDoc();
                String id = stateAbbr.toUpperCase(Locale.US) + "|" + countyFips + "|" + precinctNumber;
                precinct.setId(id);
                precinct.setStateAbbr(stateAbbr.toUpperCase(Locale.US));
                precinct.setCountyFips(countyFips);
                precinct.setCountyName(countyName);
                precinct.setPrecinctNumber(precinctNumber);
                precinct.setPrecinctName(precinctName);
                precinct.setCentroidLon(doubleValue(props, "centroid_lon"));
                precinct.setCentroidLat(doubleValue(props, "centroid_lat"));
                precinct.setGeometry(geometry(feature));

                repo.save(precinct);
                processed++;

                if (processed % 200 == 0) {
                    System.out.println("[GeoWaPrecinctsImporter] Processed: " + processed);
                }
            } catch (Exception ex) {
                skipped++;
                if (skipped <= 10) {
                    System.err.println("[GeoWaPrecinctsImporter] Error: " + ex.getMessage());
                }
            }
        }

        System.out.println("[GeoWaPrecinctsImporter] Import complete:");
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

