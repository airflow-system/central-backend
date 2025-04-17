package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.Intersection;
import com.airflow.centralbackend.Model.Location;
import com.airflow.centralbackend.Model.Trip;
import com.airflow.centralbackend.dto.Coordinate;
import com.airflow.centralbackend.dto.Route;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class MockOSMClient {
    private Random random = new Random();

    /**
     * Simulates fetching a list of intersections between the start and end locations.
     * Here, we generate a fixed number (e.g., 10) intersections with slight random offsets.
     */
    public List<Intersection> getIntersections(Route trip, Coordinate start, Location end, int totalCount) {
        List<Intersection> intersections = new ArrayList<>();
        double latStep = (end.getLatitude() - start.getLatitude()) / (totalCount + 1);
        double lonStep = (end.getLongitude() - start.getLongitude()) / (totalCount + 1);

        for (int i = 1; i <= totalCount; i++) {
            double lat = start.getLatitude() + latStep * i + random.nextDouble() * 0.005;
            double lon = start.getLongitude() + lonStep * i + random.nextDouble() * 0.005;
            Intersection intersection = new Intersection();
//            intersection.setTrip(trip);
            intersection.setSequenceNumber(i);
            intersection.setLocation(new Location(lat, lon));
            intersections.add(intersection);
        }
        return intersections;
    }
}
