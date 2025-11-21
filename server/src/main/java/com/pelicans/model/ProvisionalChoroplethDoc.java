package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Stores structured choropleth data from results_2024/*_choropleth.json files.
 *
 * Example document:
 * {
 *   "_id": "2024|E1A",
 *   "year": 2024,
 *   "measure": "E1A",
 *   "bins": [0.0, 10.0, 105.14, ...],
 *   "states": {
 *     "25": {
 *       "values": {
 *         "25025": 1800.0,
 *         "25027": 950.0
 *       }
 *     }
 *   },
 *   "createdAt": ISODate(...),
 *   "updatedAt": ISODate(...)
 * }
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "eavs_choropleth")
@CompoundIndex(name = "yr_measure_unique", def = "{'year':1,'measure':1}", unique = true)
public class ProvisionalChoroplethDoc {

    @Id
    private String id;        // e.g., "2024|E1A"
    private int year;         // e.g., 2024
    private String measure;   // e.g., "E1A"
    private List<Double> bins;  // Bin boundaries
    private Map<String, StateValues> states;  // State FIPS -> StateValues
    private Date createdAt;
    private Date updatedAt;

    public ProvisionalChoroplethDoc() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Nested class for state values
    public static class StateValues {
        private Map<String, Double> values;  // County FIPS -> value

        public Map<String, Double> getValues() {
            return values;
        }

        public void setValues(Map<String, Double> values) {
            this.values = values;
        }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }
    public List<Double> getBins() { return bins; }
    public void setBins(List<Double> bins) { this.bins = bins; }
    public Map<String, StateValues> getStates() { return states; }
    public void setStates(Map<String, StateValues> states) { this.states = states; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
