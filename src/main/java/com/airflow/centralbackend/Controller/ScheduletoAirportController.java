package com.airflow.centralbackend.Controller;

import com.airflow.centralbackend.Model.Location;
import com.airflow.centralbackend.Model.Trip;
import com.airflow.centralbackend.Services.ScheduletoAirportServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling truck scheduling and navigation to the airport.
 *
 * Endpoints:
 * 1) POST /api/airFlow/scheduletowards?truckId=...&driverId=...
 *    with a Location payload to schedule a trip.
 *
 * 2) PUT /api/airFlow/updateLocation/{tripId}
 *    with a Location payload to update the trip location.
 *
 * 3) PUT /api/airFlow/completeTrip/{tripId} to complete the trip.
 *
 * 4) GET /api/airFlow/trip/{tripId} to retrieve trip details.
 */
@RestController
@RequestMapping("/api/airFlow")
public class ScheduletoAirportController {

    @Autowired
    private ScheduletoAirportServices scheduletoAirportServices;

    /**
     * Schedules a truck for a route toward the airport.
     * This endpoint triggers the generation of the route (using Google Directions API),
     * fetches intersections via the OSM mock, obtains initial DALI advices for the first three intersections,
     * reserves a parking slot, and returns the Trip object.
     *
     * EXAMPLE:
     * POST /api/airFlow/scheduletowards?truckId=TRUCK001&driverId=DRIVER001
     * BODY: { "latitude":12.34, "longitude":56.78 }
     */
    @PostMapping("/scheduletowards")
    public Trip scheduleTruck(@RequestParam String truckId,
                              @RequestParam String driverId,
                              @RequestBody Location currentLocation) {
        return scheduletoAirportServices.scheduleTruck(truckId, driverId, currentLocation);
    }

    /**
     * Updates the trip's current location and, if necessary, recalculates the route and ETA.
     *
     * EXAMPLE:
     * PUT /api/airFlow/updateLocation/{tripId}
     * BODY: { "latitude":12.90, "longitude":56.20 }
     */
    @PutMapping("/updateLocation/{tripId}")
    public Trip updateTripLocation(@PathVariable String tripId,
                                   @RequestBody Location newLocation) {
        return scheduletoAirportServices.updateTripLocation(tripId, newLocation);
    }

    /**
     * Completes an ongoing trip. This endpoint marks the trip as inactive,
     * confirms arrival with the airport, and performs any necessary clean-up.
     *
     * EXAMPLE:
     * PUT /api/airFlow/completeTrip/{tripId}
     */
    @PutMapping("/completeTrip/{tripId}")
    public Trip completeTrip(@PathVariable String tripId) {
        return scheduletoAirportServices.completeTrip(tripId);
    }

    /**
     * Retrieves details of a trip, including transient fields such as the current route,
     * latest DALI advice, and the list of DALI advices for intersections.
     *
     * EXAMPLE:
     * GET /api/airFlow/trip/{tripId}
     */
    @GetMapping("/trip/{tripId}")
    public Trip getTrip(@PathVariable String tripId) {
        return scheduletoAirportServices.getTrip(tripId);
    }
}
