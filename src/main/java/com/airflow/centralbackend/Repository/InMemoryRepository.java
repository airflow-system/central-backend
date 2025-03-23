package com.airflow.centralbackend.Repository;

import com.airflow.centralbackend.Model.Driver;
import com.airflow.centralbackend.Model.Truck;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class InMemoryRepository {
    private Map<String, Driver> drivers = new HashMap<>();
    private Map<String, Truck> trucks = new HashMap<>();

    @PostConstruct
    public void init() {
        // Initialize some drivers
        drivers.put("DRIVER001", new Driver("DRIVER001", "Alice", "LIC12345", "9999999999"));
        drivers.put("DRIVER002", new Driver("DRIVER002", "Bob", "LIC56789", "8888888888"));

        // Initialize some trucks
        trucks.put("TRUCK001", new Truck("TRUCK001", "AB-123-CD", "VolvoX", "10T"));
        trucks.put("TRUCK002", new Truck("TRUCK002", "EF-456-GH", "FordF", "8T"));
    }

    public Driver findDriverById(String driverId) {
        return drivers.get(driverId);
    }

    public Truck findTruckById(String truckId) {
        return trucks.get(truckId);
    }
}
