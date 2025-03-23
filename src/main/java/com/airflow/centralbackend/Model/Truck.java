package com.airflow.centralbackend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trucks")
public class Truck {
    @Id
    private String truckId;
    private String licensePlate;
    private String model;
    private String capacity;

    public Truck() {}

    public Truck(String truckId, String licensePlate, String model, String capacity) {
        this.truckId = truckId;
        this.licensePlate = licensePlate;
        this.model = model;
        this.capacity = capacity;
    }

    public String getTruckId() {
        return truckId;
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
