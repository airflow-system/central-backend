package com.airflow.centralbackend.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver", referencedColumnName = "id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truck", referencedColumnName = "id")
    private Truck truck;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parking_slot", referencedColumnName = "id")
    private ParkingSlot reservedParkingSlot;

    @Embedded
    private Location currentLocation;

    private LocalDateTime startTime;
    private LocalDateTime estimatedArrivalTime;
    private boolean active;

    @Transient
    private Route currentRoute;

    @Transient
    private DaliAdvice latestDaliAdvice;

    // New transient field to hold a list of DALI advices for intersections
    @Transient
    private List<DaliAdvice> dalAdviceList;

    @Transient
    private boolean transientErrorFlag = false;

    @Transient
    private String transientErrorCode;

    @Transient
    private String errorMessage;

    // In Trip.java
    @Transient
    private List<Intersection> upcomingIntersections;

    public List<Intersection> getUpcomingIntersections() {
        return upcomingIntersections;
    }

    public void setUpcomingIntersections(List<Intersection> upcomingIntersections) {
        this.upcomingIntersections = upcomingIntersections;
    }


    public Trip() {
        this.active = true;
    }

    // Getters and setters for all fields

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Driver getDriver() {
        return driver;
    }
    public void setDriver(Driver driver) {
        this.driver = driver;
    }
    public Truck getTruck() {
        return truck;
    }
    public void setTruck(Truck truck) {
        this.truck = truck;
    }
    public ParkingSlot getReservedParkingSlot() {
        return reservedParkingSlot;
    }
    public void setReservedParkingSlot(ParkingSlot reservedParkingSlot) {
        this.reservedParkingSlot = reservedParkingSlot;
    }
    public Location getCurrentLocation() {
        return currentLocation;
    }
    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }
    public void setEstimatedArrivalTime(LocalDateTime estimatedArrivalTime) {
        this.estimatedArrivalTime = estimatedArrivalTime;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public Route getCurrentRoute() {
        return currentRoute;
    }
    public void setCurrentRoute(Route currentRoute) {
        this.currentRoute = currentRoute;
    }
    public DaliAdvice getLatestDaliAdvice() {
        return latestDaliAdvice;
    }
    public void setLatestDaliAdvice(DaliAdvice latestDaliAdvice) {
        this.latestDaliAdvice = latestDaliAdvice;
    }
    public List<DaliAdvice> getDalAdviceList() {
        return dalAdviceList;
    }
    public void setDalAdviceList(List<DaliAdvice> dalAdviceList) {
        this.dalAdviceList = dalAdviceList;
    }
    public boolean isTransientErrorFlag() {
        return transientErrorFlag;
    }
    public void setTransientErrorFlag(boolean transientErrorFlag) {
        this.transientErrorFlag = transientErrorFlag;
    }
    public String getTransientErrorCode() {
        return transientErrorCode;
    }
    public void setTransientErrorCode(String transientErrorCode) {
        this.transientErrorCode = transientErrorCode;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void completeTrip() {
        this.active = false;
    }
}
