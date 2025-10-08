package com.pelicans.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "states")
public class State {
    @Id
    private String id;          // USPS code, e.g. "NY"
    private String name;
    private GeoPoint center;    // GeoJSON Point
    private int mapZoom;
    private Date createdAt;
    private Date updatedAt;

    // ---- Geo helpers ----
    public static class GeoPoint {
        private String type;        // "Point"
        private double[] coordinates; // [lng, lat]
        public GeoPoint() {}
        public GeoPoint(double lng, double lat) {
            this.type = "Point";
            this.coordinates = new double[]{lng, lat};
        }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public double[] getCoordinates() { return coordinates; }
        public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }
    }

    // ---- getters/setters ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public GeoPoint getCenter() { return center; }
    public void setCenter(GeoPoint center) { this.center = center; }
    public int getMapZoom() { return mapZoom; }
    public void setMapZoom(int mapZoom) { this.mapZoom = mapZoom; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
