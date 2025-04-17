package com.airflow.centralbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DockResponse {
    @JsonProperty("dock_id")
    private String dockId;
    private Coordinate location;

    public DockResponse() {}

    public DockResponse(String dockId, Coordinate location) {
        this.dockId   = dockId;
        this.location = location;
    }

    public String getDockId() {
        return dockId;
    }

    public void setDockId(String dockId) {
        this.dockId = dockId;
    }

    public Coordinate getLocation() {
        return location;
    }

    public void setLocation(Coordinate location) {
        this.location = location;
    }
}
