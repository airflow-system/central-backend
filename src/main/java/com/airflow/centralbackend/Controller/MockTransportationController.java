// src/main/java/com/airflow/centralbackend/controller/MockTransportationController.java
package com.airflow.centralbackend.Controller;

import com.airflow.centralbackend.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/mock")
public class MockTransportationController {

    private static final String BASE = "https://transportation-mock-api.onrender.com";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, String> flightInfoCache = new ConcurrentHashMap<>();

    // 1) GET /api/mock/manifests
    @GetMapping("/manifests")
    public ManifestsResponse getManifests(@RequestHeader("x-token") String token) {
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<ManifestsResponse> resp = restTemplate.exchange(
                BASE + "/mock/logistics/get_manifests",
                HttpMethod.GET,
                entity,
                ManifestsResponse.class
        );
        return resp.getBody();
    }

    // 2) POST /api/mock/assign-tasks
    @PostMapping("/assign-tasks")
    public AssignmentsResponse assignTasks(@RequestHeader("x-token") String token,
                                           @RequestBody AssignTasksRequest request) throws JsonProcessingException {
        String manifestsJson = objectMapper.writeValueAsString(request.getManifests());

        String encodedJson = URLEncoder.encode(manifestsJson, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder
                .fromHttpUrl(BASE + "/mock/dispatch/assign_tasks")
                .queryParam("manifests_json", encodedJson)
                .build(true)   // <<-- true == “values are already encoded”
                .toUri();

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<AssignmentsResponse> resp = restTemplate.exchange(
                uri, HttpMethod.GET, entity, AssignmentsResponse.class);
        return resp.getBody();
    }

    @GetMapping("/intersections")
    public IntersectionsResponse getIntersections(
            @RequestHeader("x-token") String token,
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam(required = false) String routeJson
    ) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(BASE + "/mock/osm/get_intersections")
                .queryParam("start_lat", startLat)
                .queryParam("start_lon", startLon);
        if (routeJson != null) {
            b.queryParam("route_json", routeJson);
        }
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
        return restTemplate.exchange(
                b.build(true).toUri(), HttpMethod.GET, entity, IntersectionsResponse.class
        ).getBody();
    }

    // 4) GET /api/mock/dali/intersection-status
    @GetMapping("/dali/intersection-status")
    public IntersectionResponse getDaliIntersection(
            @RequestHeader("x-token") String token,
            @RequestParam String truckerId,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String priority
    ) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(BASE + "/mock/dali/intersection")
                .queryParam("trucker_id", truckerId)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("priority", priority)
                .build(true)
                .toUri();

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
        return restTemplate.exchange(
                uri, HttpMethod.GET, entity, IntersectionResponse.class
        ).getBody();
    }

    // 5) Other endpoints (accident, speed, parking, verifyparking, arrival)
    //    follow the same pattern: build the URI with query params, set headers,
    //    call restTemplate.exchange(…), and map to an appropriate DTO or Map.

    @GetMapping("/flightinfo")
    public FlighInfoResponse fetchFlightInfo(
            @RequestHeader("x-token") String token,
            @RequestParam("flight_number") String flightNumber
    ) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(BASE + "/mock/air/flightinfo")
                .queryParam("flight_number", flightNumber)
                .encode()
                .build()
                .toUri();

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<FlighInfoResponse> resp = restTemplate.exchange(
                uri, HttpMethod.GET, entity, FlighInfoResponse.class
        );
        return resp.getBody();
    }

    @GetMapping("/reserveparking")
    public ParkingResponse reserveParking(
            @RequestHeader("x-token") String token
    ) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(BASE + "/mock/air/reserveparking")
                .build()
                .toUri();

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<ParkingResponse> resp = restTemplate.exchange(
                uri, HttpMethod.GET, entity, ParkingResponse.class
        );
        return resp.getBody();
    }

    @GetMapping("/reservedock")
    public DockResponse reserveDock(
            @RequestHeader("x-token") String token,
            @RequestParam("terminal") String terminal
    ) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(BASE + "/mock/air/reservedock")
                .queryParam("terminal", terminal)
                .build(true)
                .toUri();

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<DockResponse> resp = restTemplate.exchange(
                uri, HttpMethod.GET, entity, DockResponse.class
        );
        return resp.getBody();
    }
    @GetMapping("/route_polyline")
    public String getRoutePolyline(
            @RequestHeader("x-token") String xToken,                 // your incoming header
            @RequestParam("start_lat") double startLat,
            @RequestParam("start_lon") double startLon,
            @RequestParam("end_lat")   double endLat,
            @RequestParam("end_lon")   double endLon,
            @RequestParam(value = "filter_traffic_lights", defaultValue = "true")
            boolean filterTrafficLights,
            @RequestParam(value = "avoid_tolls", defaultValue = "false")
            boolean avoidTolls
    ) {
        // Build headers for the downstream call
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", xToken);   // OpenAPI says the header name is "token"

        // Build the URI with all required & optional params
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://routes-api-bqt1.onrender.com/route_polyline")
                .queryParam("start_lat",             startLat)
                .queryParam("start_lon",             startLon)
                .queryParam("end_lat",               endLat)
                .queryParam("end_lon",               endLon)
                .queryParam("filter_traffic_lights", filterTrafficLights)
                .queryParam("avoid_tolls",           avoidTolls)
                .build()   // values get encoded automatically
                .toUri();

        // Fire the GET
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                uri, HttpMethod.GET, entity, String.class
        );
        System.out.println(resp.getBody());
        return resp.getBody();
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-token", "token");
        return headers;
    }
}
