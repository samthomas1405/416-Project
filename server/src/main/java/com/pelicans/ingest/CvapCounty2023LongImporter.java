package com.pelicans.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.pelicans.model.CvapCountyDoc;
import com.pelicans.repository.CvapCountyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
@ConditionalOnProperty(name = "cvap.county.enabled", havingValue = "true", matchIfMissing = false)
public class CvapCounty2023LongImporter implements CommandLineRunner {

    private final CvapCountyRepository repo;

    @Value("${cvap.county.input:data_clean/cvap/cvap_2019_2023_county_long.csv}")
    private String inputPath;

    public CvapCounty2023LongImporter(CvapCountyRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        java.io.File f = new java.io.File(inputPath);
        if (!f.exists()) {
            System.out.println("[CvapCounty2023LongImporter] File not found: " + f.getAbsolutePath());
            return;
        }

        System.out.println("[CvapCounty2023LongImporter] Reading CSV: " + f.getAbsolutePath());

        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            rows = reader.readAll();
        }

        if (rows.isEmpty()) {
            System.out.println("[CvapCounty2023LongImporter] CSV is empty");
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
                String fips5 = getValue(row, colIndex, "fips5");
                String cvapCategoryCode = getValue(row, colIndex, "cvap_category_code");

                if (stateAbbr == null || fips5 == null || cvapCategoryCode == null) {
                    skipped++;
                    continue;
                }

                String docId = stateAbbr.toUpperCase() + "|" + fips5 + "|" + cvapCategoryCode;

                CvapCountyDoc cvap = new CvapCountyDoc();
                cvap.setId(docId);
                cvap.setStateAbbr(stateAbbr.toUpperCase());
                cvap.setStateFips(getValue(row, colIndex, "state_fips"));
                cvap.setStateName(getValue(row, colIndex, "state_name"));
                cvap.setFips5(fips5);
                cvap.setCountyName(getValue(row, colIndex, "county_name"));
                cvap.setGeoid(getValue(row, colIndex, "geoid"));
                cvap.setCvapCategoryCode(cvapCategoryCode);
                cvap.setCvapCategory(getValue(row, colIndex, "cvap_category"));
                cvap.setTotalPopulationEst(parseInt(getValue(row, colIndex, "total_population_est")));
                cvap.setAdultPopulationEst(parseInt(getValue(row, colIndex, "adult_population_est")));
                cvap.setCitizenPopulationEst(parseInt(getValue(row, colIndex, "citizen_population_est")));
                cvap.setCvapEstimate(parseInt(getValue(row, colIndex, "cvap_estimate")));
                cvap.setTotalPopulationMoe(parseInt(getValue(row, colIndex, "total_population_moe")));
                cvap.setAdultPopulationMoe(parseInt(getValue(row, colIndex, "adult_population_moe")));
                cvap.setCitizenPopulationMoe(parseInt(getValue(row, colIndex, "citizen_population_moe")));
                cvap.setCvapMoe(parseInt(getValue(row, colIndex, "cvap_moe")));

                repo.save(cvap);
                processed++;

                if (processed % 100 == 0) {
                    System.out.println("[CvapCounty2023LongImporter] Processed: " + processed);
                }

            } catch (Exception e) {
                errors++;
                if (errors <= 10) {
                    System.err.println("[CvapCounty2023LongImporter] Error processing row " + i + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[CvapCounty2023LongImporter] Import complete:");
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



