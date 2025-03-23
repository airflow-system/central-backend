package com.airflow.centralbackend.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "parking_slots")
public class ParkingSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String gateId;
    private boolean reserved;

    public ParkingSlot() {}

    public ParkingSlot(String slotId, String gateId) {
        this.id = slotId;
        this.gateId = gateId;
        this.reserved = false;
    }

    public String getSlotId() {
        return id;
    }

    public String getGateId() {
        return gateId;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }
}
