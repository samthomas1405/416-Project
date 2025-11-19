package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.WaParticipationAgeDoc;
import com.pelicans.repository.WaParticipationAgeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "wa.participation.county.age.enabled", havingValue = "true", matchIfMissing = false)
public class WaParticipation2024CountyAgeImporter implements CommandLineRunner {

    private final WaParticipationAgeRepository repo;

    @Value("${wa.participation.county.age.input:data_clean/registration/wa_participation_2024_by_county_age.csv}")
    private String inputPath;

    public WaParticipation2024CountyAgeImporter(WaParticipationAgeRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[WaParticipation2024CountyAgeImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[WaParticipation2024CountyAgeImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[WaParticipation2024CountyAgeImporter] CSV is empty");
            return;
        }

        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = buildColumnIndex(headers);

        int processed = 0;
        int skipped = 0;
        int errors = 0;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < headers.length) {
                skipped++;
                continue;
            }

            try {
                String stateAbbr = getValue(row, colIndex, "state_abbr");
                String countyName = getValue(row, colIndex, "county_name");
                String ageGroup = getValue(row, colIndex, "age_group");
                String yearStr = getValue(row, colIndex, "year");
                if (yearStr == null) yearStr = getValue(row, colIndex, "Year");  // Fallback for capital Y
                String electionType = getValue(row, colIndex, "election_type");
                if (electionType == null) electionType = getValue(row, colIndex, "Election Type");  // Fallback for "Election Type"

                if (stateAbbr == null || countyName == null || ageGroup == null || yearStr == null || electionType == null) {
                    skipped++;
                    continue;
                }

                String docId = stateAbbr.toUpperCase() + "|" + countyName.toUpperCase() + "|" + ageGroup + "|" + yearStr + "|" + electionType;

                WaParticipationAgeDoc participation = new WaParticipationAgeDoc();
                participation.setId(docId);
                participation.setStateAbbr(stateAbbr.toUpperCase());
                participation.setCountyName(countyName);
                participation.setAgeGroup(ageGroup);
                participation.setYear(parseInt(yearStr));
                participation.setElectionType(electionType);
                participation.setTotalPopulation(parseInt(getValue(row, colIndex, "total_population")));
                participation.setTotalVoters(parseInt(getValue(row, colIndex, "total_voters")));
                participation.setRegisteredPopulationShare(parseDouble(getValue(row, colIndex, "registered_population_share")));
                participation.setVoterTurnoutShare(parseDouble(getValue(row, colIndex, "voter_turnout_share")));

                repo.save(participation);
                processed++;

                if (processed % 50 == 0) {
                    System.out.println("[WaParticipation2024CountyAgeImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[WaParticipation2024CountyAgeImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[WaParticipation2024CountyAgeImporter] Import complete:");
        System.out.println("  Processed: " + processed);
        System.out.println("  Skipped: " + skipped);
        System.out.println("  Errors: " + errors);
    }

    private String getValue(String[] row, Map<String, Integer> colIndex, String colName) {
        if (colName == null) return null;
        String lookup = colName.trim().toLowerCase();
        Integer idx = colIndex.get(lookup);
        if (idx == null) {
            String spaces = lookup.replace("_", " ");
            idx = colIndex.get(spaces);
        }
        if (idx == null) {
            String underscores = lookup.replace(" ", "_");
            idx = colIndex.get(underscores);
        }
        if (idx == null || idx >= row.length) return null;
        String val = row[idx];
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    private Integer parseInt(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }
        return colIndex;
    }
}

