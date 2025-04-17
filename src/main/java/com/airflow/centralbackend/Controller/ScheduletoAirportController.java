package com.airflow.centralbackend.Controller;

import com.airflow.centralbackend.Model.Location;
import com.airflow.centralbackend.Model.Trip;
import com.airflow.centralbackend.Services.AssignmentSchedulerService;
import com.airflow.centralbackend.Services.ScheduletoAirportServices;
import com.airflow.centralbackend.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private MockTransportationController mockTransportationController;

    @Autowired
    private AssignmentSchedulerService schedulerService;


    /**
     * Fetch and assign tasks, then return only tasks for the specified driver.
     *
     * GET  /api/airFlow/manifest?driverId=DRIVER001
     * Header:
     *   x-token: <auth-token>
     */
    @GetMapping("/todays-assignments")
    public AssignmentsResponse getTodaysAssignments() {
        return schedulerService.getCachedAssignments();
    }

    @GetMapping("/flightinfo")
    public TimeDetails flightInfo(
            @RequestHeader("x-token") String token,
            @RequestParam String assignmentId,
            @RequestParam String CurrLat,
            @RequestParam String currLon
    ) throws Exception {
        Map<String,Assignment> assignMap = schedulerService.getCachedAssignmentsMap();
        Assignment currAssignment = assignMap.get(assignmentId);
        String flightNumber = currAssignment.getFlight_number();
        TimeDetails info = schedulerService.getFlightInfo(token, flightNumber,assignmentId, CurrLat,currLon, assignmentId);
        return info;
    }

    @GetMapping("/driverassignment")
    public AssignmentsResponse getAssignmentsForTruck(
            @RequestHeader("x-token") String token,
            @RequestParam String userId,
            @RequestParam String truckId
    ) throws Exception {
        // 1) fetch the full day's assignments from the scheduler
        AssignmentsResponse all = schedulerService.getCachedAssignments();

        // 2) filter by truckId
        List<Assignment> filtered = all.getAssignments().stream()
                .filter(a -> truckId.equals(a.getTruck_id()))
                .collect(Collectors.toList());

        // 3) wrap & return
        AssignmentsResponse result = new AssignmentsResponse();
        result.setAssignments(filtered);

        ZoneId chicago = ZoneId.of("America/Chicago");
        LocalDateTime arriveBy = LocalDateTime.of(2025, 4, 17, 11, 0);
        LocalDateTime leaveAt  =
                schedulerService.calculateDeparture(32.9853, -96.7501, 32.8998, -97.0403, arriveBy, chicago);

        System.out.println("You should leave at " +
                leaveAt.format(DateTimeFormatter.ofPattern("h:mm a")));


        return result;
    }

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
