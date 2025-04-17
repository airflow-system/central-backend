package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Controller.MockTransportationController;
import com.airflow.centralbackend.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.google.maps.GeoApiContext;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AssignmentSchedulerService {

    private static final double AIRPORT_LAT = 32.988052;
    private static final double AIRPORT_LON = -96.750896;
    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private MockTransportationController mockController;

    private AssignmentsResponse assignResp;

    @Value("${mock.api.token}")
    private String apiToken;

    @Value("${api.key}")
    private String googleMapsApiKey;

    // Thread-safe cache for today's assignments
    private final Map<String,Assignment> cachedAssignments = new ConcurrentHashMap<>();
    private final Map<String, TimeDetails> flightInfoCache = new ConcurrentHashMap<>();

    /**
     * At midnight every day, fetch today's manifests then dispatch assign_tasks,
     * caching the result until 23:59:00.
     */
    @Scheduled(cron = "0 5 4 * * *")
    public void fetchAndCacheAssignments() throws JsonProcessingException {
        // 1) get all manifests
        ManifestsResponse manifestsResp = mockController.getManifests(apiToken);

        // 2) assign tasks for those manifests
        AssignTasksRequest req = new AssignTasksRequest();
        req.setManifests(manifestsResp.getManifests());

        assignResp = mockController.assignTasks(apiToken, req);


                // 3) cache the assignments
        cachedAssignments.clear();
        System.out.println("All assignments");
        for(int i =0; i<assignResp.getAssignments().size(); i++){
            String uuid = UUID.randomUUID().toString();
            Assignment assign = assignResp.getAssignments().get(i);
            assign.setId(uuid);
            cachedAssignments.put(uuid,assign);
            System.out.println(assignResp.getAssignments().get(i).getTruck_id());
        }

    }

    /**
     * Clears cache at 23:59:00 every day to prepare for next cycle.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void clearAssignmentsCache() {
        cachedAssignments.clear();
    }

    /**
     * Returns the cached AssignmentsResponse from the most recent midnight run.
     */
    public Map<String,Assignment> getCachedAssignmentsMap() {
        return cachedAssignments;
    }

    public AssignmentsResponse getCachedAssignments() {
        if(cachedAssignments == null){
            return new AssignmentsResponse();
        }
        else{
            return assignResp;
        }
    }

    public TimeDetails getFlightInfo(String token, String flightNumber, String assignmentId, String currLat, String currLon, String assignmentID) throws Exception {
        FlighInfoResponse FlightNumber = mockController.fetchFlightInfo(token, flightNumber);
        double currLatitude  = Double.parseDouble(
                currLat.trim().replace('−','-')
        );
        double currLongitude = Double.parseDouble(
                currLon.trim().replace('−','-')
        );
        TimeDetails timeDetails = new TimeDetails();
        timeDetails.setAssignmentId(assignmentID);
        timeDetails.setFlightNumber(flightNumber);
        timeDetails.setFlightTerminal(FlightNumber.getTerminal());
        Assignment assignment = cachedAssignments.get(assignmentId);

        LocalTime arrivalTime = LocalTime.parse(FlightNumber.getArrival_time(), HM);
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime targetArrival = LocalDateTime.of(today, arrivalTime);
        timeDetails.setEstimatedEndTimeFromPickUp(targetArrival.toString());
        double pickUpLatitude = assignment.getLocation().getLatitude();
        double pickUpLongitude = assignment.getLocation().getLongitude();
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (targetArrival.isBefore(now)) {
            targetArrival = targetArrival.plusDays(1);
        }
        LocalDateTime departure = calculateDeparture(
                pickUpLatitude,
                pickUpLongitude,
                AIRPORT_LAT,
                AIRPORT_LON,
                targetArrival,
                ZONE
        );
        timeDetails.setEstimatedStartTimeFromPickUp(departure.toString());
        LocalDateTime arrivalAtPickupLocation = departure.minusHours(1);
        if (arrivalAtPickupLocation.isBefore(now)) {
            arrivalAtPickupLocation = arrivalAtPickupLocation.plusDays(1);
        }

        timeDetails.setEstimatedEndTimeFromCurrent(arrivalAtPickupLocation.toString());
        System.out.println(currLatitude+" "+currLongitude );
        LocalDateTime departureFromCurr = calculateDeparture(
                currLatitude,
                currLongitude,
                pickUpLatitude,
                pickUpLongitude,
                arrivalAtPickupLocation,
                ZONE
        );
        timeDetails.setEstimatedStartTimeFromCurrent(departureFromCurr.toString());

        ParkingResponse reserveParking = mockController.reserveParking(apiToken);
        DockResponse reserveDoc = mockController.reserveDock(apiToken, timeDetails.getFlightTerminal());
        timeDetails.setParkingId(reserveParking.getParkingId());
        timeDetails.setParkinglocation(reserveParking.getLocation());
        timeDetails.setDockId(reserveDoc.getDockId());
        timeDetails.setDocklocation(reserveDoc.getLocation());
        flightInfoCache.put(assignmentID,timeDetails);
        System.out.println(reserveParking+" "+reserveDoc);
        return timeDetails;
    }

    public TimeDetails getCachedFlightInfo(String assignmentID) {
        return flightInfoCache.get(assignmentID);
    }

    public LocalDateTime calculateDeparture(
            double originLat, double originLng,
            double destLat,   double destLng,
            LocalDateTime targetArrival, ZoneId zone
    ) throws Exception {
        String origin      = originLat  + "," + originLng;
        String destination = destLat    + "," + destLng;
        Instant arrivalTs     = targetArrival.atZone(zone).toInstant();

        GeoApiContext ctx = new GeoApiContext.Builder()
                .apiKey(googleMapsApiKey)
                .build();

        DirectionsResult result = DirectionsApi.newRequest(ctx)
                .mode(TravelMode.DRIVING)
                .origin(origin)
                .destination(destination)
                .departureTime(arrivalTs)
                .await();

        long durSec = result.routes[0].legs[0].durationInTraffic.inSeconds;
        Instant departureInstant = arrivalTs.minusSeconds(durSec);
        LocalDateTime recommendedDeparture =
                LocalDateTime.ofInstant(departureInstant, zone);
        System.out.println(recommendedDeparture);
        return LocalDateTime.ofInstant(departureInstant, zone);
    }


}
