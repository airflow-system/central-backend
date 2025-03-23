package com.airflow.centralbackend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_slots")
public class ParkingSlot {
    @Id
    private String slotId;
    private String gateId;
    private boolean reserved;

    public ParkingSlot() {}

    public ParkingSlot(String slotId, String gateId) {
        this.slotId = slotId;
        this.gateId = gateId;
        this.reserved = false;
    }

    public String getSlotId() {
        return slotId;
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
