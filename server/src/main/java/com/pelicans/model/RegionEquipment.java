package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "region_equipment")
@CompoundIndex(name = "yr_state_region_unique", def = "{'year':1,'stateId':1,'regionId':1}", unique = true)
public class RegionEquipment {

    @Id
    private String id;   // "2024|NY|NY-36061"
    private Integer year;
    private String stateId;
    private String regionId;
    private List<MixItem> mix;
    private Double regionQualityScore;

    public static class MixItem {
        private String provider;
        private String model;
        private Integer count;
        private Equipment.Category category;
        private Integer firstInServiceYear;
        private Equipment.Certification certification;
        private Double scanRate;
        private Double errorRate;
        private Double reliability;
        // getters/setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Equipment.Category getCategory() { return category; }
        public void setCategory(Equipment.Category category) { this.category = category; }
        public Integer getFirstInServiceYear() { return firstInServiceYear; }
        public void setFirstInServiceYear(Integer firstInServiceYear) { this.firstInServiceYear = firstInServiceYear; }
        public Equipment.Certification getCertification() { return certification; }
        public void setCertification(Equipment.Certification certification) { this.certification = certification; }
        public Double getScanRate() { return scanRate; }
        public void setScanRate(Double scanRate) { this.scanRate = scanRate; }
        public Double getErrorRate() { return errorRate; }
        public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
        public Double getReliability() { return reliability; }
        public void setReliability(Double reliability) { this.reliability = reliability; }
    }

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public List<MixItem> getMix() { return mix; }
    public void setMix(List<MixItem> mix) { this.mix = mix; }
    public Double getRegionQualityScore() { return regionQualityScore; }
    public void setRegionQualityScore(Double regionQualityScore) { this.regionQualityScore = regionQualityScore; }
}
