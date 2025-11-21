package com.pelicans.model;

import org.bson.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.util.Date;

@org.springframework.data.mongodb.core.mapping.Document(collection = "GeoWAPrecincts")
@CompoundIndex(name = "geo_wa_precinct_unique", def = "{'countyFips':1,'precinctNumber':1,'precinctName':1}", unique = true)
public class GeoWaPrecinctDoc {

    @Id
    private String id; // stateAbbr|countyFips|precinctNumber
    private String stateAbbr;
    private String countyFips;
    private String countyName;
    private String precinctNumber;
    private String precinctName;
    private Double centroidLon;
    private Double centroidLat;
    private Document geometry;
    private Date createdAt;
    private Date updatedAt;

    public GeoWaPrecinctDoc() {
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

    public String getCountyFips() {
        return countyFips;
    }

    public void setCountyFips(String countyFips) {
        this.countyFips = countyFips;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public String getPrecinctNumber() {
        return precinctNumber;
    }

    public void setPrecinctNumber(String precinctNumber) {
        this.precinctNumber = precinctNumber;
    }

    public String getPrecinctName() {
        return precinctName;
    }

    public void setPrecinctName(String precinctName) {
        this.precinctName = precinctName;
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
