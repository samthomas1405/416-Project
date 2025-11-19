package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.PresResultsMaTownDoc;
import com.pelicans.repository.PresResultsMaTownRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "pres.results.ma.town.enabled", havingValue = "true", matchIfMissing = false)
public class PresResults2024MaTownImporter implements CommandLineRunner {

    private final PresResultsMaTownRepository repo;

    @Value("${pres.results.ma.town.input:data_clean/results_2024/pres_2024_ma_by_town.csv}")
    private String inputPath;

    public PresResults2024MaTownImporter(PresResultsMaTownRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[PresResults2024MaTownImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[PresResults2024MaTownImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[PresResults2024MaTownImporter] CSV is empty");
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
                String townName = getValue(row, colIndex, "city_town");
                if (townName == null) {
                    townName = getValue(row, colIndex, "town_name");  // Try alternative
                }

                if (stateAbbr == null || townName == null) {
                    skipped++;
                    continue;
                }

                String docId = stateAbbr.toUpperCase() + "|" + townName.toUpperCase();

                PresResultsMaTownDoc result = new PresResultsMaTownDoc();
                result.setId(docId);
                result.setStateAbbr(stateAbbr.toUpperCase());
                result.setTownName(townName);
                result.setVotesDem2024Pres(parseInt(getValue(row, colIndex, "votes_dem_2024_pres")));
                result.setVotesRep2024Pres(parseInt(getValue(row, colIndex, "votes_rep_2024_pres")));
                result.setVotesOther2024Pres(parseInt(getValue(row, colIndex, "votes_other_2024_pres")));
                result.setTotalVotes2024Pres(parseInt(getValue(row, colIndex, "total_votes_2024_pres")));
                result.setDemShare2024Pres(parseDouble(getValue(row, colIndex, "dem_share_2024_pres")));
                result.setRepShare2024Pres(parseDouble(getValue(row, colIndex, "rep_share_2024_pres")));

                repo.save(result);
                processed++;

                if (processed % 50 == 0) {
                    System.out.println("[PresResults2024MaTownImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[PresResults2024MaTownImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[PresResults2024MaTownImporter] Import complete:");
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

