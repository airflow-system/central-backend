package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.*;
import com.airflow.centralbackend.Repository.DriverRepository;
import com.airflow.centralbackend.Repository.TruckRepository;
import com.airflow.centralbackend.Repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ScheduletoAirportServices {
    private static final double AIRPORT_LAT = 32.8998;
    private static final double AIRPORT_LON = -97.0403;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private TripRepository tripRepository;

    // Use the in-memory cache for intersections instead of persisting to DB.
    @Autowired
    private IntersectionCacheService intersectionCacheService;

    // Use the provided mock clients.
    @Autowired
    private MockDaliClient mockDaliClient;

    @Autowired
    private MockAirportClient mockAirportClient;

    @Autowired
    private MockOSMClient mockOSMClient;

    @Value("${api.key}")
    private String googleMapsApiKey;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Schedules a truck for a route toward the airport.
     * Generates a route, reserves parking, obtains initial DALI advice,
     * fetches intersections via the mock OSM client, and caches them in memory.
     */
    public Trip scheduleTruck(String truckId, String driverId, Location currentLocation) {
        // Validate driver and truck.
        Optional<Driver> driverOptional = driverRepository.findById(driverId);
        if (driverOptional.isEmpty()) {
            return createErrorTrip("Driver not found in DB: " + driverId, "DRIVER_NOT_FOUND");
        }
        Driver driver = driverOptional.get();

        Optional<Truck> truckOptional = truckRepository.findById(truckId);
        if (truckOptional.isEmpty()) {
            return createErrorTrip("Truck not found in DB: " + truckId, "TRUCK_NOT_FOUND");
        }
        Truck truck = truckOptional.get();

        // Get initial DALI advice for current location.
        DaliAdvice initialAdvice = mockDaliClient.getRealTimeTrafficData(currentLocation);

        // Generate route via Google Directions API.
        Route route;
        try {
            route = fetchRouteFromGoogle(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    AIRPORT_LAT,
                    AIRPORT_LON);
            route.setRelevantLocation(new Location(AIRPORT_LAT, AIRPORT_LON));
        } catch (Exception e) {
            return createErrorTrip("Failed to fetch route: " + e.getMessage(), "ROUTE_FETCH_FAILED");
        }

        // Reserve a parking slot using the mock Airport client.
        List<ParkingSlot> availableSlots = mockAirportClient.getAvailableSlots();
        if (availableSlots.isEmpty()) {
            return createErrorTrip("No available parking slots at the airport!", "NO_PARKING_SLOTS");
        }
        ParkingSlot chosenSlot = availableSlots.get(0);
        ParkingSlot reservedSlot = mockAirportClient.reserveSlot(chosenSlot.getSlotId());
        if (reservedSlot == null) {
            return createErrorTrip("Failed to reserve parking slot!", "SLOT_RESERVATION_FAILED");
        }

        // Build and save the Trip.
        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setTruck(truck);
        trip.setReservedParkingSlot(reservedSlot);
        trip.setCurrentLocation(currentLocation);
        trip.setCurrentRoute(route);
        trip.setLatestDaliAdvice(initialAdvice);
        trip.setActive(true);
        trip.setTransientErrorFlag(false);
        LocalDateTime now = LocalDateTime.now();
        trip.setStartTime(now);
        trip.setEstimatedArrivalTime(now.plusMinutes((long) route.getEstimatedTimeMinutes()));

        Trip savedTrip;
        try {
            savedTrip = tripRepository.save(trip);
        } catch (Exception e) {
            return createErrorTrip("Failed to save trip: " + e.getMessage(), "DB_SAVE_FAILED");
        }

        // Fetch intersections using the mock OSM client.
        try {
            List<Intersection> intersections = mockOSMClient.getIntersections(savedTrip, currentLocation, new Location(AIRPORT_LAT, AIRPORT_LON), 10);
            // Store intersections in the in-memory cache.
            intersectionCacheService.putIntersections(savedTrip.getId(), intersections);
            // Retrieve the first 3 intersections.
            List<Intersection> firstThree = intersectionCacheService.getNextIntersections(savedTrip.getId(), 3);
            // For each intersection, get DALI advice and attach it.
            for (Intersection inter : firstThree) {
                DaliAdvice adviceForIntersection = mockDaliClient.getRealTimeTrafficData(inter.getLocation());
                inter.setDaliAdvice(adviceForIntersection);
            }
            // Set these intersections in the transient field so they are sent in the response.
            savedTrip.setUpcomingIntersections(firstThree);
        } catch (Exception e) {
            return createErrorTrip("Failed to fetch intersections: " + e.getMessage(), "OSM_FETCH_FAILED");
        }

        return savedTrip;
    }

    /**
     * Updates the trip location.
     * - Gets updated DALI advice.
     * - If advice indicates a route change, re-fetches the route and refreshes intersections.
     * - Otherwise, retrieves the next 3 intersections from the cache and includes them in the response.
     * - Verifies parking slot availability and reassigns if necessary.
     */
    public Trip updateTripLocation(String tripId, Location newLocation) {
        Optional<Trip> tripOptional = tripRepository.findById(tripId);
        if (tripOptional.isEmpty()) {
            return createErrorTrip("Trip not found: " + tripId, "TRIP_NOT_FOUND");
        }
        Trip trip = tripOptional.get();
        if (!trip.isActive()) {
            return createErrorTrip("Trip is already completed!", "TRIP_COMPLETED");
        }
        trip.setCurrentLocation(newLocation);

        // Log the location update (simulate sending update to DALI)
        System.out.println("[DALI] Received location update from driver " + trip.getDriver().getDriverId() +
                ": lat=" + newLocation.getLatitude() + ", lon=" + newLocation.getLongitude());

        // Get updated DALI advice for the current location
        DaliAdvice advice = mockDaliClient.getRealTimeTrafficData(newLocation);
        trip.setLatestDaliAdvice(advice);

        if (advice.isRouteChanged()) {
            try {
                // Re-fetch the route if a change is needed
                Route updatedRoute = fetchRouteFromGoogle(
                        newLocation.getLatitude(),
                        newLocation.getLongitude(),
                        AIRPORT_LAT,
                        AIRPORT_LON);
                trip.setCurrentRoute(updatedRoute);
                LocalDateTime now = LocalDateTime.now();
                trip.setEstimatedArrivalTime(now.plusMinutes((long) updatedRoute.getEstimatedTimeMinutes()));
                // Refresh intersections: clear cache and load new intersections
                intersectionCacheService.removeIntersections(trip.getId());
                List<Intersection> newIntersections = mockOSMClient.getIntersections(trip, newLocation, trip.getCurrentRoute().getRelevantLocation(), 10);
                intersectionCacheService.putIntersections(trip.getId(), newIntersections);
            } catch (Exception e) {
                return createErrorTrip("Failed to update route and refresh intersections: " + e.getMessage(), "ROUTE_UPDATE_FAILED");
            }
        } else {
            // Retrieve next three intersections from the cache
            List<Intersection> nextThree = intersectionCacheService.getNextIntersections(trip.getId(), 3);
            // For each intersection, get DALI advice and attach it
            for (Intersection inter : nextThree) {
                DaliAdvice interAdvice = mockDaliClient.getRealTimeTrafficData(inter.getLocation());
                inter.setDaliAdvice(interAdvice);
            }
            trip.setUpcomingIntersections(nextThree);
        }

        // Adjust ETA if the advice mentions a delay.
        if (advice.getMessage().toLowerCase().contains("delay")) {
            trip.setEstimatedArrivalTime(trip.getEstimatedArrivalTime().plusMinutes(5));
        }

        // Verify parking slot using the mock Airport client.
        try {
            ParkingSlot verifiedSlot = mockAirportClient.verifyParkingSlot(trip.getReservedParkingSlot().getSlotId());
            if (verifiedSlot == null) {
                List<ParkingSlot> availableSlots = mockAirportClient.getAvailableSlots();
                if (availableSlots.isEmpty()) {
                    return createErrorTrip("Parking slot is no longer available!", "PARKING_SLOT_UNAVAILABLE");
                }
                trip.setReservedParkingSlot(availableSlots.get(0));
            } else {
                trip.setReservedParkingSlot(verifiedSlot);
            }
        } catch (Exception e) {
            return createErrorTrip("Failed to verify parking slot: " + e.getMessage(), "PARKING_VERIFY_FAILED");
        }

        try {
            Trip updatedTrip = tripRepository.save(trip);
            return updatedTrip;
        } catch (Exception e) {
            return createErrorTrip("Failed to save updated trip: " + e.getMessage(), "DB_SAVE_FAILED");
        }
    }


    /**
     * Completes the trip.
     * Confirms arrival via the mock Airport client, clears the intersection cache,
     * and deletes the trip from the database.
     */
    public Trip completeTrip(String tripId) {
        Optional<Trip> tripOptional = tripRepository.findById(tripId);
        if (tripOptional.isEmpty()) {
            return createErrorTrip("Trip not found: " + tripId, "TRIP_NOT_FOUND");
        }
        Trip trip = tripOptional.get();
        if (!trip.isActive()) {
            return createErrorTrip("Trip is already completed!", "TRIP_ALREADY_COMPLETED");
        }
        trip.setActive(false);
        LocalDateTime now = LocalDateTime.now();
        // Confirm arrival using the mock Airport client.
        try {
            mockAirportClient.confirmArrival(trip.getTruck().getTruckId());
        } catch (Exception e) {
            return createErrorTrip("Failed to confirm arrival with airport: " + e.getMessage(), "AIRPORT_CONFIRM_FAILED");
        }
        // Clear intersections from the in-memory cache.
        intersectionCacheService.removeIntersections(trip.getId());
        try {
            tripRepository.delete(trip);
        } catch (Exception e) {
            return createErrorTrip("Failed to delete trip after completion: " + e.getMessage(), "DB_DELETE_FAILED");
        }
        Trip responseTrip = new Trip();
        responseTrip.setErrorMessage("Trip completed and cleaned up.");
        responseTrip.setActive(false);
        return responseTrip;
    }

    /**
     * Retrieves trip details.
     */
    public Trip getTrip(String tripId) {
        Optional<Trip> tripOptional = tripRepository.findById(tripId);
        if (tripOptional.isEmpty()) {
            return createErrorTrip("Trip not found: " + tripId, "TRIP_NOT_FOUND");
        }
        return tripOptional.get();
    }

    // -------------------- Google Directions API Methods --------------------
    // These methods remain unchanged.

    private Route fetchRouteFromGoogle(double startLat, double startLng, double endLat, double endLng)
            throws Exception {
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";
        Map<String, Object> requestBody = buildRoutesRequestBody(startLat, startLng, endLat, endLng);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", googleMapsApiKey);
        headers.set("x-goog-fieldmask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<Map> response = rt.postForEntity(url, entity, Map.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new Exception("Failed to call Google Routes API: " + response.getStatusCode());
        }
        return parseGoogleRoutesResponse(response.getBody());
    }

    private Map<String, Object> buildRoutesRequestBody(double startLat, double startLng, double endLat, double endLng) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> origin = new HashMap<>();
        Map<String, Object> originLocation = new HashMap<>();
        Map<String, Object> originLatLng = new HashMap<>();
        originLatLng.put("latitude", startLat);
        originLatLng.put("longitude", startLng);
        originLocation.put("latLng", originLatLng);
        origin.put("location", originLocation);
        requestBody.put("origin", origin);
        Map<String, Object> destination = new HashMap<>();
        Map<String, Object> destinationLocation = new HashMap<>();
        Map<String, Object> destinationLatLng = new HashMap<>();
        destinationLatLng.put("latitude", endLat);
        destinationLatLng.put("longitude", endLng);
        destinationLocation.put("latLng", destinationLatLng);
        destination.put("location", destinationLocation);
        requestBody.put("destination", destination);
        requestBody.put("travelMode", "DRIVE");
        requestBody.put("routingPreference", "TRAFFIC_AWARE");
        requestBody.put("computeAlternativeRoutes", true);
        return requestBody;
    }

    private Route parseGoogleRoutesResponse(Map<String, Object> body) throws Exception {
        List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
        if (routes == null || routes.isEmpty()) {
            throw new Exception("No routes found in the Google response");
        }
        Map<String, Object> firstRoute = routes.get(0);
        double durationMinutes = 0.0;
        Object durationField = firstRoute.get("duration");
        if (durationField != null) {
            durationMinutes = parseDurationField(durationField);
        }
        double distanceKm = 0.0;
        Object distanceField = firstRoute.get("distanceMeters");
        if (distanceField != null) {
            distanceKm = parseDistanceField(distanceField);
        }
        String polyline = null;
        Map<String, Object> polylineMap = (Map<String, Object>) firstRoute.get("polyline");
        if (polylineMap != null) {
            polyline = (String) polylineMap.get("encodedPolyline");
        }
        Route route = new Route(new ArrayList<>(), distanceKm, durationMinutes);
        route.setEncodedPolyline(polyline);
        return route;
    }

    private double parseDurationField(Object durationField) throws Exception {
        double durationSeconds = 0.0;
        if (durationField instanceof Number) {
            durationSeconds = ((Number) durationField).doubleValue();
        } else if (durationField instanceof String) {
            String durationStr = ((String) durationField).trim();
            if (durationStr.endsWith("s")) {
                durationStr = durationStr.substring(0, durationStr.length() - 1);
            }
            try {
                durationSeconds = Double.parseDouble(durationStr);
            } catch (NumberFormatException e) {
                throw new Exception("Unable to parse duration: " + durationField);
            }
        } else {
            throw new Exception("Unexpected duration field type: " + durationField.getClass().getName());
        }
        return durationSeconds / 60.0;
    }

    private double parseDistanceField(Object distanceField) throws Exception {
        double distanceMeters = 0.0;
        if (distanceField instanceof Number) {
            distanceMeters = ((Number) distanceField).doubleValue();
        } else if (distanceField instanceof String) {
            String distanceStr = ((String) distanceField).trim();
            try {
                distanceMeters = Double.parseDouble(distanceStr);
            } catch (NumberFormatException e) {
                throw new Exception("Unable to parse distanceMeters: " + distanceField);
            }
        } else {
            throw new Exception("Unexpected distance field type: " + distanceField.getClass().getName());
        }
        return distanceMeters / 1000.0;
    }

    private Trip createErrorTrip(String errorMessage, String errorCode) {
        Trip errorTrip = new Trip();
        errorTrip.setActive(false);
        errorTrip.setErrorMessage(errorMessage);
        errorTrip.setTransientErrorFlag(true);
        errorTrip.setTransientErrorCode(errorCode);
        return errorTrip;
    }
}
