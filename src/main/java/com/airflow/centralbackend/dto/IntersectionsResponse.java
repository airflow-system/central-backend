package com.airflow.centralbackend.dto;

import java.util.List;

public class IntersectionsResponse {
    private List<Coordinate> intersections;
    public IntersectionsResponse() {}
    public List<Coordinate> getIntersections() { return intersections; }
    public void setIntersections(List<Coordinate> intersections) { this.intersections = intersections; }
}