package com.airflow.centralbackend.Repository;

import com.airflow.centralbackend.Model.Intersection;
import com.airflow.centralbackend.Model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntersectionRepository extends JpaRepository<Intersection, String> {
    // Find intersections for a given trip ordered by sequence number
    List<Intersection> findByTripOrderBySequenceNumber(Trip trip);
}
