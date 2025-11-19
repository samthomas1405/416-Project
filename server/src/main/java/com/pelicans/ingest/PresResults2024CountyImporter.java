package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.PresResultsCountyDoc;
import com.pelicans.repository.PresResultsCountyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "pres.results.county.enabled", havingValue = "true", matchIfMissing = false)
public class PresResults2024CountyImporter implements CommandLineRunner {

    private final PresResultsCountyRepository repo;

    @Value("${pres.results.county.input:data_clean/results_2024/pres_2024_by_county.csv}")
    private String inputPath;

    public PresResults2024CountyImporter(PresResultsCountyRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[PresResults2024CountyImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[PresResults2024CountyImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[PresResults2024CountyImporter] CSV is empty");
            return;
        }

        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }

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
                String fips5 = getValue(row, colIndex, "fips5");

                if (stateAbbr == null || countyName == null) {
                    skipped++;
                    continue;
                }

                // Use county_name as identifier if fips5 is not available
                String identifier = (fips5 != null && !fips5.isEmpty()) ? fips5 : countyName.toUpperCase().replaceAll("\\s+", "_");
                String docId = stateAbbr.toUpperCase() + "|" + identifier;

                PresResultsCountyDoc result = new PresResultsCountyDoc();
                result.setId(docId);
                result.setStateAbbr(stateAbbr.toUpperCase());
                result.setFips5(fips5);  // May be null, that's okay
                result.setCountyName(countyName);
                result.setVotesDem2024Pres(parseInt(getValue(row, colIndex, "votes_dem_2024_pres")));
                result.setVotesRep2024Pres(parseInt(getValue(row, colIndex, "votes_rep_2024_pres")));
                result.setVotesOther2024Pres(parseInt(getValue(row, colIndex, "votes_other_2024_pres")));
                result.setTotalVotes2024Pres(parseInt(getValue(row, colIndex, "total_votes_2024_pres")));
                result.setDemShare2024Pres(parseDouble(getValue(row, colIndex, "dem_share_2024_pres")));
                result.setRepShare2024Pres(parseDouble(getValue(row, colIndex, "rep_share_2024_pres")));

                repo.save(result);
                processed++;

                if (processed % 50 == 0) {
                    System.out.println("[PresResults2024CountyImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[PresResults2024CountyImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[PresResults2024CountyImporter] Import complete:");
        System.out.println("  Processed: " + processed);
        System.out.println("  Skipped: " + skipped);
        System.out.println("  Errors: " + errors);
    }

    private String getValue(String[] row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
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
}

