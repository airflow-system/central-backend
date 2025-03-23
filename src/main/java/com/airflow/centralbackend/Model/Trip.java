package com.airflow.centralbackend.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @Column(name = "trip_id")
    private String tripId;

    // We link to the Driver entity via a foreign key column "driver_id".
    // EAGER or LAZY loading is up to you. For now, let's do EAGER.
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    // Similarly for Truck
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truck_id")
    private Truck truck;

    // If you want each Trip to hold a reserved parking slot, do the same
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parking_slot_id")
    private ParkingSlot reservedParkingSlot;

    // We'll embed the location columns directly in "trips" (latitude, longitude).
    @Embedded
    private Location currentLocation;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime estimatedArrivalTime;

    @Column
    private boolean active;

    // We do NOT store Route or DaliAdvice in DB => mark them @Transient
    @Transient
    private Route currentRoute;          // no table/column
    @Transient
    private DaliAdvice latestDaliAdvice; // no table/column

    public Trip() {
        this.active = true;
    }

    public Trip(String tripId, Driver driver, Truck truck) {
        this.tripId = tripId;
        this.driver = driver;
        this.truck = truck;
        this.active = true;
    }

    public String getTripId() {
        return tripId;
    }
    public void setTripId(String tripId) {
        this.tripId = tripId;
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
}
