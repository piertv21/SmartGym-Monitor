package com.smartgym.analyticsservice.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AttendanceSnapshot {

    private final LocalDate date;
    private final int gymCount;
    private final Map<String, Integer> areaCountByAreaId;
    private final LocalDateTime generatedAt;

    public AttendanceSnapshot(LocalDate date, int gymCount, Map<String, Integer> areaCountByAreaId, LocalDateTime generatedAt) {
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
        if (gymCount < 0) {
            throw new IllegalArgumentException("gymCount cannot be negative");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt cannot be null");
        }
        this.date = date;
        this.gymCount = gymCount;
        this.areaCountByAreaId = areaCountByAreaId == null ? Collections.emptyMap() : new HashMap<>(areaCountByAreaId);
        this.generatedAt = generatedAt;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getGymCount() {
        return gymCount;
    }

    public Map<String, Integer> getAreaCountByAreaId() {
        return Collections.unmodifiableMap(areaCountByAreaId);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}

