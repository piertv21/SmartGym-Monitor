package com.smartgym.machineservice.model;

public class Machine {

    private final String machineId;
    private final String areaId;
    private OccupancyStatus status;
    private String activeSessionId;

    public Machine(String machineId, String areaId) {
        this(machineId, areaId, OccupancyStatus.FREE);
    }

    public Machine(String machineId, String areaId, OccupancyStatus status) {
        this.machineId = requireNotBlank(machineId, "machineId");
        this.areaId = requireNotBlank(areaId, "areaId");
        this.status = status == null ? OccupancyStatus.FREE : status;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getAreaId() {
        return areaId;
    }

    public OccupancyStatus getStatus() {
        return status;
    }

    public String getActiveSessionId() {
        return activeSessionId;
    }

    public void startSession(String sessionId) {
        if (status != OccupancyStatus.FREE) {
            throw new IllegalStateException("machine is not free: " + machineId);
        }
        this.activeSessionId = requireNotBlank(sessionId, "sessionId");
        this.status = OccupancyStatus.OCCUPIED;
    }

    public void endSession(String sessionId) {
        if (status != OccupancyStatus.OCCUPIED) {
            throw new IllegalStateException("machine is not occupied: " + machineId);
        }
        if (activeSessionId == null || !activeSessionId.equals(sessionId)) {
            throw new IllegalStateException("session mismatch for machine " + machineId);
        }
        this.activeSessionId = null;
        this.status = OccupancyStatus.FREE;
    }

    public void setMaintenance() {
        if (status == OccupancyStatus.OCCUPIED) {
            throw new IllegalStateException("cannot set maintenance while occupied: " + machineId);
        }
        this.activeSessionId = null;
        this.status = OccupancyStatus.MAINTENANCE;
    }

    public void setAvailable() {
        if (status != OccupancyStatus.MAINTENANCE) {
            throw new IllegalStateException("machine is not in maintenance: " + machineId);
        }
        this.status = OccupancyStatus.FREE;
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}

