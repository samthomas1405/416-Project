package com.pelicans.model;

import org.bson.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.util.Date;

@org.springframework.data.mongodb.core.mapping.Document(collection = "GeoCounties")
@CompoundIndex(name = "geo_county_state_fips_unique", def = "{'stateAbbr':1,'fips5':1}", unique = true)
public class GeoCountyDoc {

    @Id
    private String id; // stateAbbr|fips5
    private String stateAbbr;
    private String fips5;
    private String countyName;
    private Double centroidLon;
    private Double centroidLat;
    private Document geometry;
    private Date createdAt;
    private Date updatedAt;

    public GeoCountyDoc() {
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

    public String getFips5() {
        return fips5;
    }

    public void setFips5(String fips5) {
        this.fips5 = fips5;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
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
