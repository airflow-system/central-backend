package com.airflow.centralbackend.Repository;

import com.airflow.centralbackend.Model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, String> {
    @Query("SELECT d FROM Driver d WHERE d.driverId = :driverId")
    Driver findDriverByDriverId(@Param("driverId") String driverId);
}
