package com.airflow.centralbackend.Model;

import java.util.List;

public class Route {
    private List<RouteStep> steps;
    private double totalDistanceKm;
    private double estimatedTimeMinutes;

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
}
