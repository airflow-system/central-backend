package com.airflow.centralbackend.Services;

import com.airflow.centralbackend.Model.ParkingSlot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class MockAirportClient {
    private List<ParkingSlot> mockSlots = new ArrayList<>();
    private Random random = new Random();

    public MockAirportClient() {
        // Sample data: 3 gates each with 2 slots (total 6 slots)
        mockSlots.add(new ParkingSlot("SLOT-A1", "GateA"));
        mockSlots.add(new ParkingSlot("SLOT-A2", "GateA"));
        mockSlots.add(new ParkingSlot("SLOT-B1", "GateB"));
        mockSlots.add(new ParkingSlot("SLOT-B2", "GateB"));
        mockSlots.add(new ParkingSlot("SLOT-C1", "GateC"));
        mockSlots.add(new ParkingSlot("SLOT-C2", "GateC"));
    }

    /**
     * Returns all available (non-reserved) parking slots.
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
     * Reserves the slot with the given ID if it is available.
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
     * Verifies whether the current parking slot is still valid.
     * Simulates an 80% chance that the slot remains reserved.
     * If not, it releases the current slot and reserves a new available slot.
     */
    public ParkingSlot verifyParkingSlot(String slotId) {
        // 80% chance the slot remains valid.
        if (random.nextInt(100) < 80) {
            for (ParkingSlot slot : mockSlots) {
                if (slot.getSlotId().equals(slotId)) {
                    return slot;
                }
            }
            return null;
        } else {
            // Simulate slot becoming unavailable.
            for (ParkingSlot slot : mockSlots) {
                if (slot.getSlotId().equals(slotId)) {
                    slot.setReserved(false); // release the current slot.
                    break;
                }
            }
            List<ParkingSlot> available = getAvailableSlots();
            if (!available.isEmpty()) {
                ParkingSlot newSlot = available.get(0);
                newSlot.setReserved(true);
                return newSlot;
            }
            return null; // no available slot found.
        }
    }

    public void confirmArrival(String truckId) {
        System.out.println("[AIRPORT] Truck " + truckId + " has arrived at the airport.");
    }
}
