package com.pelicans.model;

import org.bson.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;

/**
 * Stores raw choropleth JSON data from results_2024/*_choropleth.json files.
 *
 * Example document:
 * {
 *   "_id": "2024|E1A",
 *   "year": 2024,
 *   "measure": "E1A",
 *   "rawData": {
 *     "region_col": "FIPSCODE",
 *     "value_col": "E1A",
 *     "bins": [0.0, 10.0, 105.14, ...],
 *     "data": [
 *       {"FIPSCODE": "00023", "E1A": 0.0},
 *       {"FIPSCODE": "00100", "E1A": 0.0},
 *       ...
 *     ],
 *     "meta": {
 *       "source": "data/raw/eavs/2024/eavs_2024.csv",
 *       "generated_at": "2025-10-09T01:54:56.564990+00:00",
 *       "rows": 6461
 *     }
 *   }
 * }
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "eavs_choropleth")
@CompoundIndex(name = "yr_measure_unique", def = "{'year':1,'measure':1}", unique = true)
public class ProvisionalChoroplethDoc {

    @Id
    private String id;        // e.g., "2024|E1A"
    private int year;         // e.g., 2024 (extracted for indexing/querying)
    private String measure;   // e.g., "E1A" (extracted for indexing/querying)
    
    /** Raw JSON data from the choropleth file stored as MongoDB Document */
    private Document rawData;

    // --- getters/setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }
    public Document getRawData() { return rawData; }
    public void setRawData(Document rawData) { this.rawData = rawData; }
}
