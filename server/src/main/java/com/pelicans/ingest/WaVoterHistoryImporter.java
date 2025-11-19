package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.WaVoterHistoryDoc;
import com.pelicans.repository.WaVoterHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@ConditionalOnProperty(name = "wa.voter.history.enabled", havingValue = "true", matchIfMissing = false)
public class WaVoterHistoryImporter implements CommandLineRunner {

    private final WaVoterHistoryRepository repo;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Value("${wa.voter.history.input:data_clean/registration/wa_vrdb_voter_history.csv}")
    private String inputPath;

    public WaVoterHistoryImporter(WaVoterHistoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[WaVoterHistoryImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[WaVoterHistoryImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[WaVoterHistoryImporter] CSV is empty");
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
                String voterHistoryId = getValue(row, colIndex, "voter_history_id");
                if (voterHistoryId == null || voterHistoryId.isEmpty()) {
                    skipped++;
                    continue;
                }

                String docId = voterHistoryId;

                WaVoterHistoryDoc history = new WaVoterHistoryDoc();
                history.setId(docId);
                history.setStateAbbr(getValue(row, colIndex, "state_abbr"));
                history.setVoterHistoryId(voterHistoryId);
                history.setStateVoterId(getValue(row, colIndex, "state_voter_id"));
                history.setCountyCode(getValue(row, colIndex, "county_code"));
                history.setCountyCodeVoting(getValue(row, colIndex, "county_code_voting"));
                
                String electionDateStr = getValue(row, colIndex, "election_date_str");
                history.setElectionDateStr(electionDateStr);
                if (electionDateStr != null && !electionDateStr.isEmpty()) {
                    try {
                        history.setElectionDate(dateFormat.parse(electionDateStr));
                    } catch (Exception e) {
                        // Date parsing failed, continue without it
                    }
                }
                
                history.setElectionYear(parseInt(getValue(row, colIndex, "election_year")));

                repo.save(history);
                processed++;

                if (processed % 1000 == 0) {
                    System.out.println("[WaVoterHistoryImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[WaVoterHistoryImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[WaVoterHistoryImporter] Import complete:");
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

    private Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }
        return colIndex;
    }
}



