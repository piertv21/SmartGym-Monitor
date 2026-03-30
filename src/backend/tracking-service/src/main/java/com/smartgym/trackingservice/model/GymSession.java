package com.smartgym.trackingservice.model;

import java.time.LocalDateTime;

public class GymSession {

    private final String gymSessionId;
    private final String badgeId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    public GymSession(String gymSessionId, String badgeId, LocalDateTime startTime) {
        this.gymSessionId = requireNotBlank(gymSessionId, "gymSessionId");
        this.badgeId = requireNotBlank(badgeId, "badgeId");
        if (startTime == null) {
            throw new IllegalArgumentException("startTime cannot be null");
        }
        this.startTime = startTime;
    }

    public String getGymSessionId() {
        return gymSessionId;
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
            throw new IllegalStateException("gym session already ended: " + gymSessionId);
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

