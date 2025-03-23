package com.airflow.centralbackend.Repository;

import com.airflow.centralbackend.Model.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, String> {
}