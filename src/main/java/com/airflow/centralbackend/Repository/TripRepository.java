package com.airflow.centralbackend.Repository;

import com.airflow.centralbackend.Model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, String> {

    @Query("SELECT t FROM Trip t WHERE t.tripId = :tripId")
    Trip findTripByTripId(@Param("tripId") String tripId);

}