package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.Intersection;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class IntersectionCacheService {

    // Map keyed by trip ID; each value is a thread‑safe queue of Intersection objects.
    private final ConcurrentHashMap<String, Queue<Intersection>> intersectionCache = new ConcurrentHashMap<>();

    // Save intersections for a trip.
    public void putIntersections(String tripId, List<Intersection> intersections) {
        intersectionCache.put(tripId, new ConcurrentLinkedQueue<>(intersections));
    }

    // Retrieve the next batch (e.g. 3) intersections for a trip.
    public List<Intersection> getNextIntersections(String tripId, int batchSize) {
        Queue<Intersection> queue = intersectionCache.get(tripId);
        if (queue == null) {
            return Collections.emptyList();
        }
        List<Intersection> batch = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            Intersection inter = queue.poll();
            if (inter == null) break;
            batch.add(inter);
        }
        return batch;
    }

    // Remove all intersections for a trip from the cache.
    public void removeIntersections(String tripId) {
        intersectionCache.remove(tripId);
    }
}
