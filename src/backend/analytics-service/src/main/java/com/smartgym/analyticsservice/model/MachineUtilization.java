package com.smartgym.analyticsservice.model;

public class MachineUtilization {

    private final String machineId;
    private final long occupiedMinutes;
    private final long totalMinutes;

    public MachineUtilization(String machineId, long occupiedMinutes, long totalMinutes) {
        if (machineId == null || machineId.isBlank()) {
            throw new IllegalArgumentException("machineId cannot be blank");
        }
        if (occupiedMinutes < 0 || totalMinutes <= 0 || occupiedMinutes > totalMinutes) {
            throw new IllegalArgumentException("invalid utilization values");
        }
        this.machineId = machineId;
        this.occupiedMinutes = occupiedMinutes;
        this.totalMinutes = totalMinutes;
    }

    public String getMachineId() {
        return machineId;
    }

    public long getOccupiedMinutes() {
        return occupiedMinutes;
    }

    public long getTotalMinutes() {
        return totalMinutes;
    }

    public double getUtilizationRate() {
        return (double) occupiedMinutes / (double) totalMinutes;
    }
}

