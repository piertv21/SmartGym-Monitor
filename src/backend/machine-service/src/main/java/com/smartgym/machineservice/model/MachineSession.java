package com.smartgym.machineservice.model;

import com.smartgym.machineservice.ddd.Entity;
import java.time.LocalDateTime;

public class MachineSession implements Entity<String> {

    private final String sessionId;
    private final String machineId;
    private final String badgeId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    public MachineSession(
            String sessionId, String machineId, String badgeId, LocalDateTime startTime) {
        this(sessionId, machineId, badgeId, startTime, null);
    }

    public MachineSession(
            String sessionId,
            String machineId,
            String badgeId,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        this.sessionId = requireNotBlank(sessionId, "sessionId");
        this.machineId = requireNotBlank(machineId, "machineId");
        this.badgeId = requireNotBlank(badgeId, "badgeId");
        if (startTime == null) {
            throw new IllegalArgumentException("startTime cannot be null");
        }
        this.startTime = startTime;
        if (endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime cannot be before startTime");
        }
        this.endTime = endTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isActive() {
        return endTime == null;
    }

    public void end(LocalDateTime sessionEndTime) {
        if (endTime != null) {
            throw new IllegalStateException("session already ended: " + sessionId);
        }
        if (sessionEndTime == null || sessionEndTime.isBefore(startTime)) {
            throw new IllegalArgumentException("session end time is invalid");
        }
        this.endTime = sessionEndTime;
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
