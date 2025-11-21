package com.pelicans.model;

import org.bson.Document;
import org.springframework.data.annotation.Id;

import java.util.Date;

@org.springframework.data.mongodb.core.mapping.Document(collection = "GeoStates")
public class GeoStateDoc {

    @Id
    private String id; // stateFips
    private String stateAbbr;
    private String stateFips;
    private String stateName;
    private Double centroidLon;
    private Double centroidLat;
    private Document geometry;
    private Date createdAt;
    private Date updatedAt;

    public GeoStateDoc() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStateAbbr() {
        return stateAbbr;
    }

    public void setStateAbbr(String stateAbbr) {
        this.stateAbbr = stateAbbr;
    }

    public String getStateFips() {
        return stateFips;
    }

    public void setStateFips(String stateFips) {
        this.stateFips = stateFips;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public Double getCentroidLon() {
        return centroidLon;
    }

    public void setCentroidLon(Double centroidLon) {
        this.centroidLon = centroidLon;
    }

    public Double getCentroidLat() {
        return centroidLat;
    }

    public void setCentroidLat(Double centroidLat) {
        this.centroidLat = centroidLat;
    }

    public Document getGeometry() {
        return geometry;
    }

    public void setGeometry(Document geometry) {
        this.geometry = geometry;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
