package com.airflow.centralbackend.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "intersections")
public class Intersection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Linking an intersection to its Trip
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    private int sequenceNumber; // order in which the intersection appears

    @Embedded
    private Location location;

    public Intersection() {}

    public Intersection(Trip trip, int sequenceNumber, Location location) {
        this.trip = trip;
        this.sequenceNumber = sequenceNumber;
        this.location = location;
    }

    public String getId() {
        return id;
    }
    public Trip getTrip() {
        return trip;
    }
    public void setTrip(Trip trip) {
        this.trip = trip;
    }
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    public Location getLocation() {
        return location;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
}
