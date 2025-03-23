package com.airflow.centralbackend.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "trucks")
public class Truck {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String licensePlate;
    private String model;
    private String capacity;

    public Truck() {}

    public Truck(String truckId, String licensePlate, String model, String capacity) {
        this.id = truckId;
        this.licensePlate = licensePlate;
        this.model = model;
        this.capacity = capacity;
    }

    public String getTruckId() {
        return id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getModel() {
        return model;
    }

    public String getCapacity() {
        return capacity;
    }
}
