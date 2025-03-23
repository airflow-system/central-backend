package com.airflow.centralbackend.Controller;

import com.airflow.centralbackend.Model.Location;
import com.airflow.centralbackend.Model.Trip;
import com.airflow.centralbackend.Services.ScheduletoAirportServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/airFlow")
public class ScheduletoAirportController {
    @Autowired
    private ScheduletoAirportServices scheduletoAirportServices;

    /**
     * 1) Schedules a truck for a route to the airport.
     *    EXAMPLE:
     *      POST /api/airFlow/schedule?truckId=TRUCK001&driverId=DRIVER001
     *      BODY: { "latitude":12.34, "longitude":56.78 }
     */
    @PostMapping("/scheduletowards")
    public Trip scheduleTruck(@RequestParam String truckId,
                              @RequestParam String driverId,
                              @RequestBody Location currentLocation) {
        System.out.println(truckId);
        System.out.println(driverId);
        return scheduletoAirportServices.scheduleTruck(truckId, driverId, currentLocation);
    }

    /**
     * 2) Updates location of an ongoing trip.
     *    EXAMPLE:
     *      PUT /api/airFlow/updateLocation/{tripId}
     *      BODY: { "latitude":12.90, "longitude":56.20 }
     */
    @PutMapping("/updateLocation/{tripId}")
    public Trip updateTripLocation(@PathVariable String tripId, @RequestBody Location newLocation) {
        return scheduletoAirportServices.updateTripLocation(tripId, newLocation);
    }

    /**
     * 3) Completes a trip (e.g. once the truck arrives).
     *    EXAMPLE:
     *      PUT /api/airFlow/completeTrip/{tripId}
     */
    @PutMapping("/completeTrip/{tripId}")
    public Trip completeTrip(@PathVariable String tripId) {
        return scheduletoAirportServices.completeTrip(tripId);
    }

    /**
     * 4) Retrieves details of a trip
     *    GET /api/airFlow/trip/{tripId}
     */
    @GetMapping("/trip/{tripId}")
    public Trip getTrip(@PathVariable String tripId) {
        return scheduletoAirportServices.getTrip(tripId);
    }
}
