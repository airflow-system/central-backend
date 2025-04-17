package com.airflow.centralbackend.dto;

import java.util.List;

public class RoutePolylineResponse {
    private String polyline;                // e.g. encoded polyline string
    private List<Coordinate> intersections; // list of lat/lon coords

    public RoutePolylineResponse() {}

    public String getPolyline() {
        return polyline;
    }
    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }

    public List<Coordinate> getIntersections() {
        return intersections;
    }
    public void setIntersections(List<Coordinate> intersections) {
        this.intersections = intersections;
    }
}