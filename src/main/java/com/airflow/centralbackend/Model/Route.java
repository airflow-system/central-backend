package com.airflow.centralbackend.Model;

import java.util.List;

public class Route {
    private List<RouteStep> steps;
    private double totalDistanceKm;
    private double estimatedTimeMinutes;
    private Location relevantLocation;
    private String encodedPolyline;

    public Route() {}

    public Route(List<RouteStep> steps, double totalDistanceKm, double estimatedTimeMinutes) {
        this.steps = steps;
        this.totalDistanceKm = totalDistanceKm;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public double getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }
    public Location getRelevantLocation() { 
        return relevantLocation; 
    }
    public void setRelevantLocation(Location relevantLocation) {
        this.relevantLocation = relevantLocation;
    }

    public String getEncodedPolyline() {
        return encodedPolyline;
    }
    
    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }
}
