package com.smartgym.machineservice.model;

import com.smartgym.machineservice.ddd.Aggregate;

public class Machine implements Aggregate<String> {

    private final String machineId;
    private final String areaId;
    private OccupancyStatus status;
    private String activeSessionId;
    private Sensor sensor;

    public Machine(String machineId, String areaId) {
        this(machineId, areaId, OccupancyStatus.FREE, null, null);
    }

    public Machine(String machineId, String areaId, Sensor sensor) {
        this(machineId, areaId, OccupancyStatus.FREE, null, sensor);
    }

    public Machine(String machineId, String areaId, OccupancyStatus status) {
        this(machineId, areaId, status, null, null);
    }

    public Machine(String machineId, String areaId, OccupancyStatus status, Sensor sensor) {
        this(machineId, areaId, status, null, sensor);
    }

    public Machine(String machineId, String areaId, OccupancyStatus status, String activeSessionId) {
        this(machineId, areaId, status, activeSessionId, null);
    }

    public Machine(String machineId, String areaId, OccupancyStatus status, String activeSessionId, Sensor sensor) {
        this.machineId = requireNotBlank(machineId, "machineId");
        this.areaId = requireNotBlank(areaId, "areaId");
        this.status = status == null ? OccupancyStatus.FREE : status;
        this.activeSessionId = normalizeBlank(activeSessionId);
        this.sensor = sensor;

        if (this.status == OccupancyStatus.OCCUPIED && this.activeSessionId == null) {
            throw new IllegalArgumentException("activeSessionId is required when machine is occupied");
        }
        if (this.status != OccupancyStatus.OCCUPIED && this.activeSessionId != null) {
            throw new IllegalArgumentException("activeSessionId must be null when machine is not occupied");
        }
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

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
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

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Machine machine = (Machine) o;
        return machineId.equals(machine.machineId) && areaId.equals(machine.areaId) && status == machine.status && activeSessionId.equals(machine.activeSessionId) && sensor.equals(machine.sensor);
    }

    @Override
    public int hashCode() {
        int result = machineId.hashCode();
        result = 31 * result + areaId.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + activeSessionId.hashCode();
        result = 31 * result + sensor.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Machine{" +
                "machineId='" + machineId + '\'' +
                ", areaId='" + areaId + '\'' +
                ", status=" + status +
                ", activeSessionId='" + activeSessionId + '\'' +
                ", sensor=" + sensor +
                '}';
    }

    @Override
    public String getId() {
        return this.machineId;
    }
}

