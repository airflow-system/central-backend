package com.airflow.centralbackend.dto;

public class TimeDetails {
    String assignmentId;
    String flightTerminal;
    String flightNumber;
    String flighInfo;
    String EstimatedStartTimeFromCurrent;
    String EstimatedEndTimeFromCurrent;
    String EstimatedStartTimeFromPickUp;
    String EstimatedEndTimeFromPickUp;
    String parkingId;
    Coordinate Parkinglocation;
    String dockId;
    Coordinate Docklocation;

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getParkingId() {
        return parkingId;
    }

    public void setParkingId(String parkingId) {
        this.parkingId = parkingId;
    }

    public Coordinate getParkinglocation() {
        return Parkinglocation;
    }

    public void setParkinglocation(Coordinate parkinglocation) {
        Parkinglocation = parkinglocation;
    }

    public String getDockId() {
        return dockId;
    }

    public void setDockId(String dockId) {
        this.dockId = dockId;
    }

    public Coordinate getDocklocation() {
        return Docklocation;
    }

    public void setDocklocation(Coordinate location) {
        this.Docklocation = location;
    }

    public String getFlightTerminal() {
        return flightTerminal;
    }

    public void setFlightTerminal(String flightName) {
        this.flightTerminal = flightName;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getFlighInfo() {
        return flighInfo;
    }

    public void setFlighInfo(String flighInfo) {
        this.flighInfo = flighInfo;
    }

    public String getEstimatedStartTimeFromCurrent() {
        return EstimatedStartTimeFromCurrent;
    }

    public void setEstimatedStartTimeFromCurrent(String estimatedStartTimeFromCurrent) {
        EstimatedStartTimeFromCurrent = estimatedStartTimeFromCurrent;
    }

    public String getEstimatedEndTimeFromCurrent() {
        return EstimatedEndTimeFromCurrent;
    }

    public void setEstimatedEndTimeFromCurrent(String estimatedEndTimeFromCurrent) {
        EstimatedEndTimeFromCurrent = estimatedEndTimeFromCurrent;
    }

    public String getEstimatedStartTimeFromPickUp() {
        return EstimatedStartTimeFromPickUp;
    }

    public void setEstimatedStartTimeFromPickUp(String estimatedStartTimeFromPickUp) {
        EstimatedStartTimeFromPickUp = estimatedStartTimeFromPickUp;
    }

    public String getEstimatedEndTimeFromPickUp() {
        return EstimatedEndTimeFromPickUp;
    }

    public void setEstimatedEndTimeFromPickUp(String estimatedEndTimeFromPickUp) {
        EstimatedEndTimeFromPickUp = estimatedEndTimeFromPickUp;
    }
}
