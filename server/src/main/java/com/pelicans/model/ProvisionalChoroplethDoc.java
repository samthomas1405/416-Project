package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal POJO for GUI05_provisional_choropleth_e1a-style data.
 *
 * Example document:
 * {
 *   "year": 2024,
 *   "measure": "E1A",
 *   "bins": 7,
 *   "states": {
 *     "25": { "values": { "25025": 1800, "25027": 950 } },
 *     "19": { "values": { "19153": 480, "19113": 360 } }
 *   }
 * }
 */
@Document(collection = "eavs_choropleth")
@CompoundIndex(name = "yr_measure_unique", def = "{'year':1,'measure':1}", unique = true)
public class ProvisionalChoroplethDoc {

    @Id
    private String id;        // e.g., "2024|E1A"
    private int year;         // e.g., 2024
    private String measure;   // e.g., "E1A"
    private int bins;         // number of bins (integer count)

    /** Map keyed by 2-digit state FIPS -> { values: { countyFips: metric } } */
    private Map<String, StateValues> states = new HashMap<>();

    // --- nested ---
    public static class StateValues {
        private Map<String, Number> values = new HashMap<>();
        public Map<String, Number> getValues() { return values; }
        public void setValues(Map<String, Number> values) { this.values = values; }
    }

    // --- getters/setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }
    public int getBins() { return bins; }
    public void setBins(int bins) { this.bins = bins; }
    public Map<String, StateValues> getStates() { return states; }
    public void setStates(Map<String, StateValues> states) { this.states = states; }
}
