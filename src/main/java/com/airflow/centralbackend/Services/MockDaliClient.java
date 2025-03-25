package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.DaliAdvice;
import com.airflow.centralbackend.Model.Location;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MockDaliClient {
    private Random random = new Random();

    /**
     * Retrieve traffic advice for a given location.
     * Could mention upcoming traffic lights, accidents, etc.
     */
    public DaliAdvice getRealTimeTrafficData(Location currentLocation) {
        // This is a mock. We'll randomly produce messages
        int r = random.nextInt(3);
        switch(r) {
            case 0:
                return new DaliAdvice("Maintain ~50 km/h to pass next light while green.", false, "INFO", null);
            case 1:
                return new DaliAdvice("Possible slow traffic ahead. Expect 5 min delay.", false, "INFO", null);
            default:
                // occasionally trigger route change
                Location accidentLoc = new Location(currentLocation.getLatitude()+0.18, currentLocation.getLongitude()-0.18);
                return new DaliAdvice("Accident reported ahead, route changed!", true, "WARNING", accidentLoc);
        }
    }

    public void sendLocationUpdateToDali(Location currentLocation, String driverId) {
        System.out.println("[DALI] Received location from driver " + driverId
                + ": lat=" + currentLocation.getLatitude()
                + ", lon=" + currentLocation.getLongitude());
    }

}
