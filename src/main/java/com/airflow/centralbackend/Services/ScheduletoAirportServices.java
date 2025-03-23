package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.*;
import com.airflow.centralbackend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
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
    @Autowired
    private InMemoryRepository inMemoryRepository;

    @Autowired
    private MockDaliClient mockDaliClient;

    @Autowired
    private MockAirportClient mockAirportClient;


    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private TripRepository tripRepository;

    // Google Directions settings
    @Value("${api.key}")
    private String GOOGLE_MAPS_API_KEY;
    private static final String GOOGLE_DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";

    /**
     * Schedules a truck for a route to the airport:
     *  1) Validates driver/truck in the DB
     *  2) Fetches DALI traffic info
     *  3) Calls Google Directions for a route
     *  4) Queries Airport for available parking
     *  5) Reserves a slot
     *  6) Persists the Trip in the DB (with Route & DALI advice in memory only)
     */
    public Trip scheduleTruck(String truckId, String driverId, Location currentLocation) {
        // 1) Validate driver & truck
//        System.out.println(driverId);
        Driver driver = driverRepository.findById(driverId).orElseThrow(
                () -> new RuntimeException("Driver not found in DB: " + driverId)
        );
//        Driver driver = drivers.get(0);
        Truck truck = truckRepository.findById(truckId).orElseThrow(
                () -> new RuntimeException("Truck not found in DB: " + truckId)
        );

        // 2) DALI traffic data
        DaliAdvice advice = mockDaliClient.getRealTimeTrafficData(currentLocation);
//        System.out.println("[DALI Info] " + advice.getMessage());
//
//        System.out.println(currentLocation.getLatitude());
//        System.out.println(currentLocation.getLongitude());
        // 3) Call Google Directions to generate a route
        Route route = fetchRouteFromGoogle(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                32.8998,      // airport lat (placeholder)
                -97.0403      // airport lon (placeholder)
        );

        // 4) Get available parking slots from Airport
        List<ParkingSlot> availableSlots = mockAirportClient.getAvailableSlots();
        if (availableSlots.isEmpty()) {
            throw new RuntimeException("No available parking slots at the airport!");
        }

        // 5) Reserve the first slot for demo
        ParkingSlot chosenSlot = availableSlots.get(0);
        ParkingSlot reservedSlot = mockAirportClient.reserveSlot(chosenSlot.getSlotId());
        if (reservedSlot == null) {
            throw new RuntimeException("Failed to reserve the parking slot!");
        }
        // Optionally, if you also store ParkingSlot in your DB, mark it reserved there:
        //   ParkingSlot ps = parkingSlotRepository.findById(chosenSlot.getSlotId()).get();
        //   ps.setReserved(true);
        //   parkingSlotRepository.save(ps);

        // 6) Build a new Trip entity
//        String tripId = UUID.randomUUID().toString();
        Trip trip = new Trip();
//        trip.setId(tripId);
        trip.setDriver(driver);
//        System.out.println(trip.getDriver());
        trip.setTruck(truck);
        trip.setReservedParkingSlot(chosenSlot);
        trip.setCurrentLocation(currentLocation);
        trip.setCurrentRoute(route);       // transient
        trip.setLatestDaliAdvice(advice);  // transient
        trip.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        trip.setStartTime(now);
        trip.setEstimatedArrivalTime(now.plusMinutes((long) route.getEstimatedTimeMinutes()));

        // Save to DB

        Trip tripNow = tripRepository.save(trip);
//        System.out.println(tripNow.getId() +" "+ tripNow.getDriver());
        return tripNow;
    }

    /**
     * Updates the trip location, obtains new DALI instructions,
     * possibly re-calculates the route if DALI indicates a route change.
     */
    public Trip updateTripLocation(String tripId, Location newLocation) {
        // Retrieve from DB
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new RuntimeException("Trip not found: " + tripId)
        );

//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        System.out.println("Update Trip trip"+trip);
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        if (!trip.isActive()) {
            throw new RuntimeException("Trip is already completed!");
        }

        // 1) Update location in the entity
        trip.setCurrentLocation(newLocation);

        // 2) Send location to DALI
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        System.out.println("Update Trip Driver:- -----"+trip.getDriver());
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        mockDaliClient.sendLocationUpdateToDali(newLocation, trip.getDriver().getDriverId());

        // 3) Get new traffic advice
        DaliAdvice advice = mockDaliClient.getRealTimeTrafficData(newLocation);
        trip.setLatestDaliAdvice(advice);  // transient
        System.out.println("[DALI Info] " + advice.getMessage());

        // 4) If route changed, re-fetch from Google
        if (advice.isRouteChanged()) {
            Route updatedRoute = fetchRouteFromGoogle(
                    newLocation.getLatitude(),
                    newLocation.getLongitude(),
                    32.8998,      // airport lat (placeholder)
                    -97.0403    // airport lon
            );
            trip.setCurrentRoute(updatedRoute); // transient

            // Recompute ETA from now
            LocalDateTime now = LocalDateTime.now();
            trip.setEstimatedArrivalTime(now.plusMinutes((long) updatedRoute.getEstimatedTimeMinutes()));
        } else {
            // If DALI mentions a delay, add 5 minutes
            if (advice.getMessage().contains("delay")) {
                trip.setEstimatedArrivalTime(trip.getEstimatedArrivalTime().plusMinutes(5));
            }
        }

        // 5) Save updated Trip in DB (location, times, etc.)
        tripRepository.save(trip);

        return trip;
    }

    /**
     * Completes the trip (marks as inactive) once the truck arrives.
     */
    public Trip completeTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new RuntimeException("Trip not found: " + tripId)
        );
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        System.out.println("completeTrip:------"+trip);
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        if (!trip.isActive()) {
            throw new RuntimeException("Trip is already completed!");
        }
        // Mark inactive
        trip.setActive(false);

        // Airport confirm
        mockAirportClient.confirmArrival(trip.getTruck().getTruckId());

        // Save final state
        tripRepository.save(trip);

        return trip;
    }

    /**
     * Retrieves a Trip by ID from the DB. This includes
     * driver/truck/parking references, but DOES NOT have
     * the ephemeral Route or DaliAdvice (since they are transient).
     */
    public Trip getTrip(String tripId) {

        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new RuntimeException("Trip not found: " + tripId)
        );

//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        System.out.println("Get Trip"+trip);
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        return trip;

    }

    /**
     * Calls Google Directions API to fetch a route with steps, distances, durations.
     * This is unchanged from your original approach, except we do not store
     * anything in an in-memory Map. The resulting Route is stored only in memory
     * (transient in the Trip entity).
     */
//    private Route fetchRouteFromGoogle(double startLat, double startLng,
//                                       double endLat, double endLng) {
//
//        String url = String.format(
//                "%s?origin=%f,%f&destination=%f,%f&key=%s",
//                GOOGLE_DIRECTIONS_URL,
//                startLat, startLng,
//                endLat, endLng,
//                GOOGLE_MAPS_API_KEY
//        );
//
//        RestTemplate restTemplate = new RestTemplate();
//        ResponseEntity<Map> response = restTemplate.exchange(
//                url,
//                HttpMethod.GET,
//                null,
//                Map.class
//        );
//
//        if (response.getStatusCode() != HttpStatus.OK) {
//            throw new RuntimeException("Failed to call Google Directions API: " + response.getStatusCode());
//        }
//
//        Map<String, Object> body = response.getBody();
//        System.out.println("Google Directions API raw response: " + body);
//        if (body == null || body.get("routes") == null) {
//            throw new RuntimeException("No routes returned from Google Directions API");
//        }
//
//        List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
//
//        if (routes.isEmpty()) {
//            throw new RuntimeException("No routes found in the Google response");
//        }
//
//        Map<String, Object> firstRoute = routes.get(0);
//        List<Map<String, Object>> legs = (List<Map<String, Object>>) firstRoute.get("legs");
//        if (legs.isEmpty()) {
//            throw new RuntimeException("No legs found in the route");
//        }
//
//        Map<String, Object> leg = legs.get(0);
//        Map<String, Object> distanceObj = (Map<String, Object>) leg.get("distance");
//        Map<String, Object> durationObj = (Map<String, Object>) leg.get("duration");
//
//        double distanceKm = 0.0;
//        double durationMinutes = 0.0;
//
//        if (distanceObj != null) {
//            Number distanceVal = (Number) distanceObj.get("value"); // meters
//            distanceKm = distanceVal.doubleValue() / 1000.0;
//        }
//        if (durationObj != null) {
//            Number durationVal = (Number) durationObj.get("value"); // seconds
//            durationMinutes = durationVal.doubleValue() / 60.0;
//        }
//
//        // Build steps
//        List<Map<String, Object>> stepList = (List<Map<String, Object>>) leg.get("steps");
//        List<RouteStep> routeSteps = new ArrayList<>();
//
//        if (stepList != null) {
//            for (Map<String, Object> step : stepList) {
//                String htmlInstruction = (String) step.get("html_instructions");
//                String instruction = htmlInstruction.replaceAll("<[^>]*>", "");
//                Map<String, Object> startLoc = (Map<String, Object>) step.get("start_location");
//                double lat = ((Number) startLoc.get("lat")).doubleValue();
//                double lng = ((Number) startLoc.get("lng")).doubleValue();
//
//                routeSteps.add(new RouteStep(instruction, new Location(lat, lng)));
//            }
//        }
//        return new Route(routeSteps, distanceKm, durationMinutes);
//    }
    private Route fetchRouteFromGoogle(double startLat, double startLng,
                                       double endLat, double endLng) {

        // New Routes API endpoint
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";

        // Build the JSON request body according to the new API specification
        Map<String, Object> requestBody = new HashMap<>();

        // Build origin
        Map<String, Object> origin = new HashMap<>();
        Map<String, Object> originLocation = new HashMap<>();
        Map<String, Object> originLatLng = new HashMap<>();
        originLatLng.put("latitude", startLat);
        originLatLng.put("longitude", startLng);
        originLocation.put("latLng", originLatLng);
        origin.put("location", originLocation);
        requestBody.put("origin", origin);

        // Build destination
        Map<String, Object> destination = new HashMap<>();
        Map<String, Object> destinationLocation = new HashMap<>();
        Map<String, Object> destinationLatLng = new HashMap<>();
        destinationLatLng.put("latitude", endLat);
        destinationLatLng.put("longitude", endLng);
        destinationLocation.put("latLng", destinationLatLng);
        destination.put("location", destinationLocation);
        requestBody.put("destination", destination);

        // Additional required parameters
        requestBody.put("travelMode", "DRIVE");
        requestBody.put("routingPreference", "TRAFFIC_AWARE");
        requestBody.put("computeAlternativeRoutes", true);

        // Create HTTP headers and add the API key and field mask
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", GOOGLE_MAPS_API_KEY);
        headers.set("x-goog-fieldmask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline");
        // For testing; production: specify only required fields

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        System.out.println("Google Routes API raw response: " + response);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to call Google Routes API: " + response.getStatusCode());
        }

        Map<String, Object> body = response.getBody();
        System.out.println("Google Routes API raw response: " + body);

        // The new API response structure: check for a "routes" field.
        List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("No routes found in the Google response");
        }

        // For demonstration, take the first route.
        Map<String, Object> firstRoute = routes.get(0);

        // Extract duration and distance if available (the field names may vary; adjust as per documentation)
        double durationMinutes = 0.0;
        double distanceKm = 0.0;

        Object durationField = firstRoute.get("duration");
        if (durationField != null) {
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
                    throw new RuntimeException("Unable to parse duration: " + durationField, e);
                }
            }
            durationMinutes = durationSeconds / 60.0;
        }
        Object distanceField = firstRoute.get("distanceMeters");
        if (distanceField != null) {
            double distanceMeters = 0.0;
            if (distanceField instanceof Number) {
                distanceMeters = ((Number) distanceField).doubleValue();
            } else if (distanceField instanceof String) {
                String distanceStr = ((String) distanceField).trim();
                try {
                    distanceMeters = Double.parseDouble(distanceStr);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Unable to parse distanceMeters: " + distanceField, e);
                }
            }
            distanceKm = distanceMeters / 1000.0;
        }
//        if (firstRoute.get("duration") != null) {
//            Number durationVal = (Number) firstRoute.get("duration");
//            durationMinutes = durationVal.doubleValue() / 60.0;
//        }
//        if (firstRoute.get("distanceMeters") != null) {
//            Number distanceVal = (Number) firstRoute.get("distanceMeters");
//            distanceKm = distanceVal.doubleValue() / 1000.0;
//        }

        // Build steps if available – for simplicity, we'll leave it empty.
        List<RouteStep> routeSteps = new ArrayList<>();

        return new Route(routeSteps, distanceKm, durationMinutes);
    }


}
