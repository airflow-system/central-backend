package com.airflow.centralbackend.Model;

public class DaliAdvice {
    private String message;          // e.g. "Maintain ~50 km/h."
    private boolean routeChanged;    // if the route is changed or not
    private String severity;         // e.g. "INFO", "WARNING", etc.
    private Location relevantLocation; // optional: location of a traffic light or accident

    public DaliAdvice() {}

    public DaliAdvice(String message, boolean routeChanged, String severity, Location relevantLocation) {
        this.message = message;
        this.routeChanged = routeChanged;
        this.severity = severity;
        this.relevantLocation = relevantLocation;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRouteChanged() {
        return routeChanged;
    }

    public String getSeverity() {
        return severity;
    }

    public Location getRelevantLocation() {
        return relevantLocation;
    }
}
