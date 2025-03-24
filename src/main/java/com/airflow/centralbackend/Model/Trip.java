package com.airflow.centralbackend.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Linking to Driver entity.
    // The join column "driver" in the trips table will reference the "id" column in
    // the driver table.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver", referencedColumnName = "id")
    private Driver driver;

    // Linking to Truck entity.
    // The join column "truck" in the trips table will reference the "id" column in
    // the truck table.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truck", referencedColumnName = "id")
    private Truck truck;

    // Linking to ParkingSlot entity.
    // The join column "parking_slot" in the trips table will reference the "id"
    // column in the parking_slots table.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parking_slot", referencedColumnName = "id")
    private ParkingSlot reservedParkingSlot;

    // Embedded location details.
    @Embedded
    private Location currentLocation;

    private LocalDateTime startTime;
    private LocalDateTime estimatedArrivalTime;
    private boolean active;

    // These fields are not persisted.
    @Transient
    private Route currentRoute;
    @Transient
    private DaliAdvice latestDaliAdvice;

    @Transient
    private boolean transientErrorFlag = false;

    @Transient
    private String transientErrorCode;

    @Transient
    private String errorMessage;

    public Trip() {
        this.active = true;
    }

    public Trip(String id, Driver driver, Truck truck) {
        this.id = id;
        this.driver = driver;
        this.truck = truck;
        this.active = true;
    }

    // Use getId() / setId() for the primary key, aligning with your updated models.
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

    public void completeTrip() {
        this.active = false;
    }

    @Transient
    public boolean isTransientErrorFlag() {
        return transientErrorFlag;
    }

    public void setTransientErrorFlag(boolean transientErrorFlag) {
        this.transientErrorFlag = transientErrorFlag;
    }

    @Transient
    public String getTransientErrorCode() {
        return transientErrorCode;
    }

    public void setTransientErrorCode(String transientErrorCode) {
        this.transientErrorCode = transientErrorCode;
    }

    @Transient
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
