package com.airflow.centralbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParkingResponse {
    @JsonProperty("parking_id")
    private String parkingId;
    private Coordinate location;

    public ParkingResponse() {}

    public ParkingResponse(String parkingId, Coordinate location) {
        this.parkingId = parkingId;
        this.location  = location;
    }

    public String getParkingId() {
        return parkingId;
    }

    public void setParkingId(String parkingId) {
        this.parkingId = parkingId;
    }

    public Coordinate getLocation() {
        return location;
    }

    public void setLocation(Coordinate location) {
        this.location = location;
    }
}