package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.DaliAdvice;
import com.airflow.centralbackend.Model.Location;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class MockDaliClient {
    private Random random = new Random();

    // A diverse list of DALI advices simulating real-world conditions.
    private List<DaliAdvice> adviceOptions = Arrays.asList(
            new DaliAdvice("Maintain ~50 km/h to pass next light while green.", false, "INFO", null),
            new DaliAdvice("Traffic congestion ahead; expect 5 min delay.", false, "INFO", null),
            new DaliAdvice("Optimal speed is 60 km/h for current road conditions.", false, "INFO", null),
            new DaliAdvice("Road closure reported ahead, change route immediately.", true, "WARNING", null),
            new DaliAdvice("Accident reported at next intersection, consider alternate route.", true, "WARNING", null),
            new DaliAdvice("Expect a brief stop at traffic light, maintain 40 km/h.", false, "INFO", null)
    );

    public DaliAdvice getRealTimeTrafficData(Location currentLocation) {
        // Randomly select one advice from the list.
        int index = random.nextInt(adviceOptions.size());
        DaliAdvice selected = adviceOptions.get(index);

        // If a route change is indicated, provide a mock location for the event.
        if (selected.isRouteChanged()) {
            double offsetLat = currentLocation.getLatitude() + 0.02 + random.nextDouble() * 0.01;
            double offsetLon = currentLocation.getLongitude() - 0.02 - random.nextDouble() * 0.01;
            return new DaliAdvice(selected.getMessage(), true, selected.getSeverity(), new Location(offsetLat, offsetLon));
        } else {
            return selected;
        }
    }

    public void sendLocationUpdateToDali(Location currentLocation, String driverId) {
        System.out.println("[DALI] Received location update from driver " + driverId +
                ": lat=" + currentLocation.getLatitude() + ", lon=" + currentLocation.getLongitude());
    }
}
