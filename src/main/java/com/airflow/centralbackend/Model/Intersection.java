package com.airflow.centralbackend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "intersections")
public class Intersection implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JsonIgnore  // Prevents recursive/nested serialization of the Trip in the Intersection response
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    private int sequenceNumber; // the order in which the intersection appears

    @Embedded
    private Location location;

    @Transient
    private DaliAdvice daliAdvice;  // transient field to hold DALI advice for this intersection

    public Intersection() {}

    public Intersection(Trip trip, int sequenceNumber, Location location) {
        this.trip = trip;
        this.sequenceNumber = sequenceNumber;
        this.location = location;
    }

    // getters and setters

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

    public DaliAdvice getDaliAdvice() {
        return daliAdvice;
    }

    public void setDaliAdvice(DaliAdvice daliAdvice) {
        this.daliAdvice = daliAdvice;
    }
}
