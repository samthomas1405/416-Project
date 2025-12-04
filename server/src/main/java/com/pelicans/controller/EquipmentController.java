package com.pelicans.controller;

import com.pelicans.model.EquipmentDeviceDoc;
import com.pelicans.repository.EquipmentDeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentDeviceRepository repo;

    @Autowired
    public EquipmentController(EquipmentDeviceRepository repo) {
        this.repo = repo;
    }

    /**
     * GUI-6: State voting equipment summary
     * Returns aggregated equipment data grouped by manufacturer and model for a state
     * 
     * @param stateAbbr 2-letter state abbreviation (e.g., "MA", "WA")
     * @return List of equipment summaries grouped by manufacturer and model
     */
    @Cacheable
    @GetMapping("/state/{stateAbbr}/summary")
    public ResponseEntity<List<EquipmentSummaryDTO>> getStateEquipmentSummary(@PathVariable String stateAbbr) {
        try {
            List<EquipmentDeviceDoc> equipmentList = repo.findByStateAbbr(stateAbbr.toUpperCase());
            
            if (equipmentList.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Group by manufacturer and model
            Map<String, List<EquipmentDeviceDoc>> grouped = equipmentList.stream()
                .collect(Collectors.groupingBy(eq -> 
                    (eq.getManufacturer() != null ? eq.getManufacturer() : "Unknown") + "|" + 
                    (eq.getModel() != null ? eq.getModel() : "Unknown")
                ));

            // Convert to summary DTOs
            List<EquipmentSummaryDTO> summaries = new ArrayList<>();
            int currentYear = 2024; // Use 2024 as reference year for age calculation

            for (Map.Entry<String, List<EquipmentDeviceDoc>> entry : grouped.entrySet()) {
                List<EquipmentDeviceDoc> devices = entry.getValue();
                if (devices.isEmpty()) continue;

                EquipmentDeviceDoc firstDevice = devices.get(0);
                
                // Calculate quantity (count of unique devices)
                int quantity = devices.size();
                
                // Calculate age (average age of devices)
                OptionalDouble avgAge = devices.stream()
                    .filter(d -> d.getFirstYearInUse() != null)
                    .mapToInt(d -> currentYear - d.getFirstYearInUse())
                    .average();
                
                Integer age = avgAge.isPresent() ? (int) Math.round(avgAge.getAsDouble()) : null;
                
                // Determine if discontinued (heuristic: if all devices are older than 10 years)
                boolean isDiscontinued = devices.stream()
                    .filter(d -> d.getFirstYearInUse() != null)
                    .allMatch(d -> (currentYear - d.getFirstYearInUse()) > 10);
                
                // Get most common equipment type
                String equipmentType = devices.stream()
                    .map(EquipmentDeviceDoc::getEquipmentType)
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                
                // Get average quality score
                OptionalDouble avgQuality = devices.stream()
                    .filter(d -> d.getQualityScore() != null)
                    .mapToDouble(EquipmentDeviceDoc::getQualityScore)
                    .average();
                
                Double qualityScore = avgQuality.isPresent() ? avgQuality.getAsDouble() : null;
                
                // Extract certification from extraText if available
                String certification = extractCertification(devices);
                
                // Create short description
                String shortDescription = createShortDescription(firstDevice, equipmentType);
                
                EquipmentSummaryDTO summary = new EquipmentSummaryDTO();
                summary.setManufacturer(firstDevice.getManufacturer());
                summary.setModel(firstDevice.getModel());
                summary.setQuantity(quantity);
                summary.setEquipmentType(equipmentType);
                summary.setShortDescription(shortDescription);
                summary.setAge(age);
                summary.setCertification(certification);
                summary.setQualityScore(qualityScore);
                summary.setDiscontinued(isDiscontinued);
                // Note: OS, scan rate, error rate, reliability not available in current data model
                // These would need to be added to the model or extracted from extraText/Google Sheet
                
                summaries.add(summary);
            }
            
            // Sort by manufacturer, then model
            summaries.sort(Comparator
                .comparing(EquipmentSummaryDTO::getManufacturer, Comparator.nullsLast(String::compareTo))
                .thenComparing(EquipmentSummaryDTO::getModel, Comparator.nullsLast(String::compareTo)));
            
            return ResponseEntity.ok(summaries);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Extract certification information from equipment extraText or infer from quality score
     */
    private String extractCertification(List<EquipmentDeviceDoc> devices) {
        // Try to extract from extraText first
        for (EquipmentDeviceDoc device : devices) {
            if (device.getExtraText() != null) {
                String extraText = device.getExtraText().toUpperCase();
                if (extraText.contains("VVSG 2.0 CERTIFIED") || extraText.contains("VVSG 2.0 CERT")) {
                    return "VVSG 2.0 certified";
                }
                if (extraText.contains("VVSG 2.0 APPLIED")) {
                    return "VVSG 2.0 applied";
                }
                if (extraText.contains("VVSG 1.1 CERTIFIED") || extraText.contains("VVSG 1.1 CERT")) {
                    return "VVSG 1.1 certified";
                }
                if (extraText.contains("VVSG 1.0 CERTIFIED") || extraText.contains("VVSG 1.0 CERT")) {
                    return "VVSG 1.0 certified";
                }
            }
        }
        
        // If not found in extraText, infer from quality score
        // High quality score (>= 0.8) likely means VVSG 2.0 certified
        // Medium (0.5-0.8) might be VVSG 1.1 or 1.0
        // Low (< 0.5) might be not certified
        OptionalDouble avgQuality = devices.stream()
            .filter(d -> d.getQualityScore() != null)
            .mapToDouble(EquipmentDeviceDoc::getQualityScore)
            .average();
        
        if (avgQuality.isPresent()) {
            double quality = avgQuality.getAsDouble();
            if (quality >= 0.8) {
                return "VVSG 2.0 certified";
            } else if (quality >= 0.5) {
                return "VVSG 1.1 certified";
            } else if (quality >= 0.3) {
                return "VVSG 1.0 certified";
            }
        }
        
        return "Not certified";
    }

    /**
     * Create a short description from equipment attributes
     */
    private String createShortDescription(EquipmentDeviceDoc device, String equipmentType) {
        StringBuilder desc = new StringBuilder();
        
        if (equipmentType != null) {
            desc.append(equipmentType);
        }
        
        if (device.getVppat() != null && device.getVppat().equalsIgnoreCase("yes")) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("with VVPAT");
        }
        
        if (device.getBarcode() != null && device.getBarcode().equalsIgnoreCase("yes")) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("barcode capable");
        }
        
        if (desc.length() == 0) {
            desc.append("Voting equipment");
        }
        
        return desc.toString();
    }

    /**
     * DTO for equipment summary response
     */
    public static class EquipmentSummaryDTO {
        private String manufacturer;
        private String model;
        private Integer quantity;
        private String equipmentType;
        private String shortDescription;
        private Integer age; // Age in years (2024 - firstYearInUse)
        private String certification; // VVSG 2.0 certified, VVSG 2.0 applied, VVSG 1.1 certified, VVSG 1.0 certified, Not certified
        private Double qualityScore;
        private Boolean discontinued; // true if device is no longer available from manufacturer
        // Note: The following fields are not available in current data model:
        // private String underlyingOS;
        // private Double scanRate;
        // private Double errorRate;
        // private Double reliability;

        // Getters and setters
        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public String getEquipmentType() { return equipmentType; }
        public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
        
        public String getShortDescription() { return shortDescription; }
        public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public String getCertification() { return certification; }
        public void setCertification(String certification) { this.certification = certification; }
        
        public Double getQualityScore() { return qualityScore; }
        public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
        
        public Boolean getDiscontinued() { return discontinued; }
        public void setDiscontinued(Boolean discontinued) { this.discontinued = discontinued; }
    }
}

