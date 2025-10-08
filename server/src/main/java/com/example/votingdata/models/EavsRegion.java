package com.example.votingdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "eavs_regions")
@CompoundIndex(name = "stateId_idx", def = "{'stateId': 1}")
public class EavsRegion {
    @Id
    private String id;            // "NY-36061"
    private String stateId;
    private String name;
    private String type;          // "county" | "town"
    private Map<String, Object> geometry; // GeoJSON Polygon/MultiPolygon
    private State.GeoPoint centroid;      // reuse GeoPoint
    private Map<String, Object> attrs;
    private Date createdAt;
    private Date updatedAt;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, Object> getGeometry() { return geometry; }
    public void setGeometry(Map<String, Object> geometry) { this.geometry = geometry; }
    public State.GeoPoint getCentroid() { return centroid; }
    public void setCentroid(State.GeoPoint centroid) { this.centroid = centroid; }
    public Map<String, Object> getAttrs() { return attrs; }
    public void setAttrs(Map<String, Object> attrs) { this.attrs = attrs; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
