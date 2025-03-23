package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.ParkingSlot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates calls to an external Airport system
 * to retrieve or reserve parking slots, confirm arrivals, etc.
 */
@Component
public class MockAirportClient {
    private List<ParkingSlot> mockSlots = new ArrayList<>();

    public MockAirportClient() {
        // Sample data: 3 gates each with 2 slots
        mockSlots.add(new ParkingSlot("SLOT-A1", "GateA"));
        mockSlots.add(new ParkingSlot("SLOT-A2", "GateA"));
        mockSlots.add(new ParkingSlot("SLOT-B1", "GateB"));
        mockSlots.add(new ParkingSlot("SLOT-B2", "GateB"));
        mockSlots.add(new ParkingSlot("SLOT-C1", "GateC"));
        mockSlots.add(new ParkingSlot("SLOT-C2", "GateC"));
    }

    /**
     * Return all available (unreserved) slots.
     */
    public List<ParkingSlot> getAvailableSlots() {
        List<ParkingSlot> available = new ArrayList<>();
        for (ParkingSlot slot : mockSlots) {
            if (!slot.isReserved()) {
                available.add(slot);
            }
        }
        return available;
    }

    /**
     * Reserve a particular slot if available. Return the updated slot or null if fail.
     */
    public ParkingSlot reserveSlot(String slotId) {
        for (ParkingSlot slot : mockSlots) {
            if (slot.getSlotId().equals(slotId) && !slot.isReserved()) {
                slot.setReserved(true);
                return slot;
            }
        }
        return null;
    }

    /**
     * Simulate the airport acknowledging the truck arrival
     */
    public void confirmArrival(String truckId) {
        System.out.println("[AIRPORT] Received notice that truck " + truckId + " has arrived.");
    }
}
