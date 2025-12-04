package com.pelicans.controller;

import com.pelicans.model.EavsDoc;
import com.pelicans.repository.EavsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/eavs")
public class EavsController {

    private static final int DEFAULT_YEAR = 2024;
    private static final int DEFAULT_CHOROPLETH_BINS = 7;
    private static final int SENTINEL_VALUE = -1;

    private final EavsRepository repo;

    @Autowired
    public EavsController(EavsRepository repo) {
        this.repo = repo;
    }

    private List<EavsDoc.Provisional> getProvisionalsFromDocs(List<EavsDoc> docs) {
        return docs.stream()
            .map(EavsDoc::getProvisional)
            .collect(Collectors.toList());
    }

    /**
     * GUI-4: Provisional ballot table
     * Returns provisional ballot data by EAVS geographic region
     * Defaults to 2024 data (as per GUI-2: "categories of 2024 EAVS data")
     * 
     * @param stateFips State FIPS code (e.g., "25" for Massachusetts)
     * @param year Optional year parameter (defaults to 2024)
     * @return List of Provisional objects with jurisdictionName and provisionalBallotCategories (E2A-E2I)
     */
    @Cacheable
    @GetMapping("/provisional/{stateFips}/regions")
    public List<EavsDoc.Provisional> getProvisionalByState(
            @PathVariable String stateFips,
            @RequestParam(required = false) Integer year) {
        try {
            Integer stateFipsInt = Integer.parseInt(stateFips);
            int yearToUse = (year != null) ? year : DEFAULT_YEAR;
            List<EavsDoc> docs = repo.findByYearAndStateFips(yearToUse, stateFipsInt);
            List<EavsDoc.Provisional> provisionals = getProvisionalsFromDocs(docs);
            return provisionals;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stateFips: " + stateFips + " (must be an integer)");
        }
    }

    /**
     * GUI-7: Display 2024 EAVS active voters
     * Returns active/inactive voter data by EAVS geographic region
     * 
     * @param stateFips State FIPS code (e.g., "25" for Massachusetts)
     * @param year Optional year parameter (defaults to 2024)
     * @return Map with "totals" (active, inactive, total) and "regions" (list of region data)
     */
    @Cacheable
    @GetMapping("/active/{stateFips}/regions")
    public Map<String, Object> getActiveVotersByState(
            @PathVariable String stateFips,
            @RequestParam(required = false) Integer year) {
        try {
            Integer stateFipsInt = Integer.parseInt(stateFips);
            int yearToUse = (year != null) ? year : DEFAULT_YEAR;
            List<EavsDoc> docs = repo.findByYearAndStateFips(yearToUse, stateFipsInt);
            
            List<Map<String, Object>> regions = new java.util.ArrayList<>();
            int totalActive = 0;
            int totalInactive = 0;
            int totalRegistered = 0;
            
            for (EavsDoc doc : docs) {
                if (doc.getRegistration() == null || doc.getRegistration().getTotalRegistered() == null) {
                    continue;
                }
                
                Map<String, Integer> totalReg = doc.getRegistration().getTotalRegistered();
                Integer total = getValueCaseInsensitive(totalReg, "A1A");
                Integer active = getValueCaseInsensitive(totalReg, "A1B");
                Integer inactive = getValueCaseInsensitive(totalReg, "A1C");
                
                if (total > 0 && active >= 0 && inactive >= 0) {
                    totalActive += active;
                    totalInactive += inactive;
                    totalRegistered += total;
                    
                    Map<String, Object> region = new HashMap<>();
                    // Use document ID for unique identifier (includes full FIPSCode for MA towns)
                    // For display, use fips5 (county-level) or full FIPSCode depending on state
                    String uniqueId = doc.getId();
                    String displayId = doc.getFips5();
                    if (displayId == null && doc.getFipscode() != null) {
                        String fipsCode = doc.getFipscode().replace(".0", "").replace(".", "");
                        // For MA, use full FIPSCode; for others, use first 5 digits
                        if (doc.getStateFips() != null && doc.getStateFips() == 25) {
                            displayId = fipsCode;
                        } else if (fipsCode.length() >= 5) {
                            displayId = fipsCode.substring(0, 5);
                        }
                    }
                    region.put("id", uniqueId);
                    region.put("fips5", displayId);
                    region.put("region", doc.getJurisdictionName() != null ? doc.getJurisdictionName() : "Unknown");
                    region.put("active", active);
                    region.put("inactive", inactive);
                    region.put("total", total);
                    region.put("pctActive", total > 0 ? (double) active / total : 0.0);
                    regions.add(region);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Integer> totals = new HashMap<>();
            totals.put("active", totalActive);
            totals.put("inactive", totalInactive);
            totals.put("total", totalRegistered);
            response.put("totals", totals);
            response.put("regions", regions);
            
            return response;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stateFips: " + stateFips + " (must be an integer)");
        }
    }

    /**
     * GUI-7: Active voters choropleth data
     * Returns percentage of active registered voters by region for choropleth map
     * 
     * @param stateFips State FIPS code (e.g., "25" for Massachusetts)
     * @param year Optional year parameter (defaults to 2024)
     * @return Map with "values" (fips5 -> percentage) and "bins" (number of bins)
     */
    @Cacheable
    @GetMapping("/active/{stateFips}/choropleth")
    public Map<String, Object> getActiveVotersChoropleth(
            @PathVariable String stateFips,
            @RequestParam(required = false) Integer year) {
        try {
            Integer stateFipsInt = Integer.parseInt(stateFips);
            int yearToUse = (year != null) ? year : DEFAULT_YEAR;
            List<EavsDoc> docs = repo.findByYearAndStateFips(yearToUse, stateFipsInt);
            
            Map<String, Long> countyActive = new HashMap<>();
            Map<String, Long> countyTotal = new HashMap<>();
            
            for (EavsDoc doc : docs) {
                if (doc.getRegistration() == null || doc.getRegistration().getTotalRegistered() == null) {
                    continue;
                }
                
                Map<String, Integer> totalReg = doc.getRegistration().getTotalRegistered();
                Integer total = getValueCaseInsensitive(totalReg, "A1A");
                Integer active = getValueCaseInsensitive(totalReg, "A1B");
                
                if (total > 0 && active >= 0) {
                    String countyFips = getCountyFips(doc);
                    if (countyFips != null) {
                        countyActive.put(countyFips, countyActive.getOrDefault(countyFips, 0L) + active);
                        countyTotal.put(countyFips, countyTotal.getOrDefault(countyFips, 0L) + total);
                    }
                }
            }
            
            Map<String, Double> values = new HashMap<>();
            for (String countyFips : countyTotal.keySet()) {
                long total = countyTotal.get(countyFips);
                long active = countyActive.getOrDefault(countyFips, 0L);
                if (total > 0) {
                    values.put(countyFips, (double) active / total);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("values", values);
            response.put("bins", DEFAULT_CHOROPLETH_BINS);
            
            return response;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stateFips: " + stateFips + " (must be an integer)");
        }
    }
    
    private String getCountyFips(EavsDoc doc) {
        if (doc.getFips5() != null && doc.getFips5().length() >= 5) {
            return doc.getFips5().substring(0, 5);
        }
        if (doc.getFipscode() != null) {
            String fipsCode = doc.getFipscode().replace(".0", "").replace(".", "");
            if (fipsCode.length() >= 5) {
                return fipsCode.substring(0, 5);
            }
        }
        if (doc.getId() != null) {
            String[] parts = doc.getId().split("\\|");
            if (parts.length >= 3) {
                String fipsPart = parts[2];
                if (fipsPart.length() >= 5) {
                    return fipsPart.substring(0, 5);
                }
            }
        }
        return null;
    }

    private Integer getValueCaseInsensitive(Map<String, Integer> map, String key) {
        if (map == null || key == null) {
            return SENTINEL_VALUE;
        }
        
        Integer value = map.get(key);
        if (value != null && value > 0) {
            return value;
        }
        
        value = map.get(key.toLowerCase());
        if (value != null && value > 0) {
            return value;
        }
        
        value = map.get(key.toUpperCase());
        if (value != null && value > 0) {
            return value;
        }
        
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                Integer val = entry.getValue();
                if (val != null && val > 0) {
                    return val;
                }
            }
        }
        
        return SENTINEL_VALUE;
    }

}
