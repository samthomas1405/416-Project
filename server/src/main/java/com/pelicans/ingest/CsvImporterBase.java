package com.pelicans.ingest;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class with common CSV parsing utilities for importers
 */
public abstract class CsvImporterBase {
    
    protected String getValue(String[] row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null || idx >= row.length) return null;
        String val = row[idx];
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    protected Integer parseInt(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Boolean parseBoolean(String s) {
        if (s == null || s.isEmpty()) return null;
        String lower = s.toLowerCase().trim();
        return lower.equals("true") || lower.equals("yes") || lower.equals("1");
    }

    protected Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim().toLowerCase(), i);
        }
        return colIndex;
    }
}



