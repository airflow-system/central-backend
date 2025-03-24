package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.*;
import com.airflow.centralbackend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ScheduletoAirportServices {
    private static final double AIRPORT_LAT = 32.8998;
    private static final double AIRPORT_LON = -97.0403;

    @Autowired
    private MockDaliClient mockDaliClient;

    @Autowired
    private MockAirportClient mockAirportClient;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private TripRepository tripRepository;

    @Value("${api.key}")
    private String googleMapsApiKey;

    /**
     * Schedules a truck for a route to the airport
     */
    public Trip scheduleTruck(String truckId, String driverId, Location currentLocation) {
        // 1) Validate driver & truck
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

        // 2) DALI traffic data
        DaliAdvice advice = mockDaliClient.getRealTimeTrafficData(currentLocation);

        // 3) Call Google Directions to generate a route
        Route route;
        try {
            route = fetchRouteFromGoogle(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    AIRPORT_LAT,
                    AIRPORT_LON);
        } catch (Exception e) {
            return createErrorTrip("Failed to fetch route: " + e.getMessage(), "ROUTE_FETCH_FAILED");
        }

        // 4) Get available parking slots from Airport
        List<ParkingSlot> availableSlots = mockAirportClient.getAvailableSlots();
        if (availableSlots.isEmpty()) {
            return createErrorTrip("No available parking slots at the airport!", "NO_PARKING_SLOTS");
        }

        // 5) Reserve the first slot
        ParkingSlot chosenSlot = availableSlots.get(0);
        ParkingSlot reservedSlot = mockAirportClient.reserveSlot(chosenSlot.getSlotId());
        if (reservedSlot == null) {
            return createErrorTrip("Failed to reserve the parking slot!", "SLOT_RESERVATION_FAILED");
        }

        // 6) Build and save the Trip entity
        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setTruck(truck);
        trip.setReservedParkingSlot(chosenSlot);
        trip.setCurrentLocation(currentLocation);
        trip.setCurrentRoute(route);
        trip.setLatestDaliAdvice(advice);
        trip.setActive(true);
        trip.setTransientErrorFlag(false);

        LocalDateTime now = LocalDateTime.now();
        trip.setStartTime(now);
        trip.setEstimatedArrivalTime(now.plusMinutes((long) route.getEstimatedTimeMinutes()));

        try {
            Trip savedTrip = tripRepository.save(trip);
            savedTrip.setTransientErrorFlag(false);
            return savedTrip;
        } catch (Exception e) {
            return createErrorTrip("Failed to save trip: " + e.getMessage(), "DB_SAVE_FAILED");
        }
    }

    /**
     * Updates the trip location and recalculates route if needed
     */
    public Trip updateTripLocation(String tripId, Location newLocation) {
        // Retrieve from DB
        Optional<Trip> tripOptional = tripRepository.findById(tripId);
        if (tripOptional.isEmpty()) {
            return createErrorTrip("Trip not found: " + tripId, "TRIP_NOT_FOUND");
        }

        Trip trip = tripOptional.get();

        // Check if trip is active
        if (!trip.isActive()) {
            return createErrorTrip("Trip is already completed!", "TRIP_COMPLETED");
        }

        // 1) Update location in the entity
        trip.setCurrentLocation(newLocation);

        // 2) Send location to DALI
        try {
            mockDaliClient.sendLocationUpdateToDali(newLocation, trip.getDriver().getDriverId());
        } catch (Exception e) {
            return createErrorTrip("Failed to send location to DALI: " + e.getMessage(), "DALI_UPDATE_FAILED");
        }

        // 3) Get new traffic advice
        DaliAdvice advice;
        try {
            advice = mockDaliClient.getRealTimeTrafficData(newLocation);
            trip.setLatestDaliAdvice(advice);
        } catch (Exception e) {
            return createErrorTrip("Failed to get traffic data from DALI: " + e.getMessage(), "DALI_DATA_FAILED");
        }

        // 4) If route changed, re-fetch from Google
        if (advice.isRouteChanged()) {
            try {
                Route updatedRoute = fetchRouteFromGoogle(
                        newLocation.getLatitude(),
                        newLocation.getLongitude(),
                        AIRPORT_LAT,
                        AIRPORT_LON);
                trip.setCurrentRoute(updatedRoute);

                // Recompute ETA
                LocalDateTime now = LocalDateTime.now();
                trip.setEstimatedArrivalTime(now.plusMinutes((long) updatedRoute.getEstimatedTimeMinutes()));
            } catch (Exception e) {
                return createErrorTrip("Failed to update route: " + e.getMessage(), "ROUTE_UPDATE_FAILED");
            }
        } else if (advice.getMessage().contains("delay")) {
            // Add delay to ETA if DALI mentions it
            trip.setEstimatedArrivalTime(trip.getEstimatedArrivalTime().plusMinutes(5));
        }

        // 5) Save updated Trip
        try {
            Trip updatedTrip = tripRepository.save(trip);
            updatedTrip.setTransientErrorFlag(false);
            return updatedTrip;
        } catch (Exception e) {
            return createErrorTrip("Failed to save updated trip: " + e.getMessage(), "DB_SAVE_FAILED");
        }
    }

    /**
     * Completes the trip
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

        try {
            mockAirportClient.confirmArrival(trip.getTruck().getTruckId());
        } catch (Exception e) {
            return createErrorTrip("Failed to confirm arrival with airport: " + e.getMessage(),
                    "AIRPORT_CONFIRM_FAILED");
        }

        try {
            Trip updatedTrip = tripRepository.save(trip);
            updatedTrip.setTransientErrorFlag(false);
            return updatedTrip;
        } catch (Exception e) {
            return createErrorTrip("Failed to save completed trip: " + e.getMessage(), "DB_SAVE_FAILED");
        }
    }

    /**
     * Retrieves a Trip by ID
     */
    public Trip getTrip(String tripId) {
        Optional<Trip> tripOptional = tripRepository.findById(tripId);
        if (tripOptional.isEmpty()) {
            return createErrorTrip("Trip not found: " + tripId, "TRIP_NOT_FOUND");
        }

        Trip trip = tripOptional.get();
        trip.setTransientErrorFlag(false);
        return trip;
    }

    /**
     * Creates a Trip object representing an error state
     */
    private Trip createErrorTrip(String errorMessage, String errorCode) {
        Trip errorTrip = new Trip();
        errorTrip.setActive(false);
        errorTrip.setErrorMessage(errorMessage);
        errorTrip.setTransientErrorFlag(true);
        errorTrip.setTransientErrorCode(errorCode);
        return errorTrip;
    }

    /**
     * Calls Google Directions API to fetch a route
     */
    private Route fetchRouteFromGoogle(double startLat, double startLng, double endLat, double endLng)
            throws Exception {
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";
        Map<String, Object> requestBody = buildRoutesRequestBody(startLat, startLng, endLat, endLng);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", googleMapsApiKey);
        headers.set("x-goog-fieldmask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("Failed to call Google Routes API: " + response.getStatusCode());
            }

            return parseGoogleRoutesResponse(response.getBody());
        } catch (Exception e) {
            throw new Exception("Google API request failed: " + e.getMessage());
        }
    }

    /**
     * Builds the request body for Google Routes API
     */
    private Map<String, Object> buildRoutesRequestBody(double startLat, double startLng, double endLat, double endLng) {
        Map<String, Object> requestBody = new HashMap<>();

        // Origin
        Map<String, Object> origin = new HashMap<>();
        Map<String, Object> originLocation = new HashMap<>();
        Map<String, Object> originLatLng = new HashMap<>();
        originLatLng.put("latitude", startLat);
        originLatLng.put("longitude", startLng);
        originLocation.put("latLng", originLatLng);
        origin.put("location", originLocation);
        requestBody.put("origin", origin);

        // Destination
        Map<String, Object> destination = new HashMap<>();
        Map<String, Object> destinationLocation = new HashMap<>();
        Map<String, Object> destinationLatLng = new HashMap<>();
        destinationLatLng.put("latitude", endLat);
        destinationLatLng.put("longitude", endLng);
        destinationLocation.put("latLng", destinationLatLng);
        destination.put("location", destinationLocation);
        requestBody.put("destination", destination);

        // Parameters
        requestBody.put("travelMode", "DRIVE");
        requestBody.put("routingPreference", "TRAFFIC_AWARE");
        requestBody.put("computeAlternativeRoutes", true);

        return requestBody;
    }

    /**
     * Parses the response from Google Routes API
     */
    private Route parseGoogleRoutesResponse(Map<String, Object> body) throws Exception {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");

        if (routes == null || routes.isEmpty()) {
            throw new Exception("No routes found in the Google response");
        }

        Map<String, Object> firstRoute = routes.get(0);

        // Extract duration
        double durationMinutes = 0.0;
        Object durationField = firstRoute.get("duration");
        if (durationField != null) {
            durationMinutes = parseDurationField(durationField);
        }

        // Extract distance
        double distanceKm = 0.0;
        Object distanceField = firstRoute.get("distanceMeters");
        if (distanceField != null) {
            distanceKm = parseDistanceField(distanceField);
        }

        return new Route(new ArrayList<>(), distanceKm, durationMinutes);
    }

    /**
     * Parses the duration field from the Google Routes response
     */
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

        return durationSeconds / 60.0; // Convert to minutes
    }

    /**
     * Parses the distance field from the Google Routes response
     */
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

        return distanceMeters / 1000.0; // Convert to kilometers
    }
}