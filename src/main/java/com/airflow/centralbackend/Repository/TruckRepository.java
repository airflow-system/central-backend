package com.airflow.centralbackend.Repository;

import com.airflow.centralbackend.Model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckRepository extends JpaRepository<Truck, String> {
}