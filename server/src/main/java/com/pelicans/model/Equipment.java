package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "equipment")
@CompoundIndex(name = "provider_model_idx", def = "{'provider':1,'model':1}")
public class Equipment {

    public Equipment(String model, int quantity, String os, Certification certification, boolean isDiscontinued) {
        this.model = model;
        this.quantity = quantity;
        this.os = os;
        this.certification = certification;
        this.isDiscontinued = isDiscontinued;
    }

    public enum Category { scanner, DRE_no_VVPAT, DRE_with_VVPAT, BMD }
    public enum Certification { VVSG_2_0_cert, VVSG_2_0_applied, VVSG_1_1, VVSG_1_0, none }

    @Id
    private String id;                 // "NY|ES&S|DS200"
    private String stateId;
    private String provider;
    private String model;
    private Category category;
    private Map<String, Integer> quantityByYear;
    private Map<String, Object> specs;
    private Integer firstInServiceYear;
    private Certification certification;
    private Boolean isDiscontinued;
    private Double qualityScore;
    private Date createdAt;
    private Date updatedAt;
    public Integer quantity;
    private String os;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Map<String, Integer> getQuantityByYear() { return quantityByYear; }
    public void setQuantityByYear(Map<String, Integer> quantityByYear) { this.quantityByYear = quantityByYear; }
    public Map<String, Object> getSpecs() { return specs; }
    public void setSpecs(Map<String, Object> specs) { this.specs = specs; }
    public Integer getFirstInServiceYear() { return firstInServiceYear; }
    public void setFirstInServiceYear(Integer firstInServiceYear) { this.firstInServiceYear = firstInServiceYear; }
    public Certification getCertification() { return certification; }
    public void setCertification(Certification certification) { this.certification = certification; }
    public Boolean getIsDiscontinued() { return isDiscontinued; }
    public void setIsDiscontinued(Boolean isDiscontinued) { this.isDiscontinued = isDiscontinued; }
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
}
