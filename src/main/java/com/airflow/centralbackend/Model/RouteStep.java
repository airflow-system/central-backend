package com.airflow.centralbackend.Model;

public class RouteStep {
    private String instruction;
    private Location location; // where this step occurs

    public RouteStep() {}

    public RouteStep(String instruction, Location location) {
        this.instruction = instruction;
        this.location = location;
    }

    public String getInstruction() {
        return instruction;
    }

    public Location getLocation() {
        return location;
    }
}
