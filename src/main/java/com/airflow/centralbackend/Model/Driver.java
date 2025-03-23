package com.airflow.centralbackend.Model;


import jakarta.persistence.*;

@Entity
@Table(name = "driver")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String licenseNumber;
    private String phoneNumber;

    public Driver() {}

    public Driver(String driverId, String name, String licenseNumber, String phoneNumber) {
        this.id = driverId;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.phoneNumber = phoneNumber;
    }

    public String getDriverId() {
        return id;
    }

    public void setDriverId(String driverId) {
        this.id = driverId;
    }

    public String getName() {
        return name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
