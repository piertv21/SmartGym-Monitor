package com.smartgym.areaservice.model;

import java.util.Objects;

public class GymArea {

    private final String areaId;
    private final String name;
    private final AreaType type;
    private int capacity;
    private int currentCount;

    public GymArea(String areaId, String name, AreaType type, int capacity) {
        this(areaId, name, type, capacity, 0);
    }

    public GymArea(String areaId, String name, AreaType type, int capacity, int currentCount) {
        this.areaId = requireNotBlank(areaId, "areaId");
        this.name = requireNotBlank(name, "name");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (currentCount < 0 || currentCount > capacity) {
            throw new IllegalArgumentException("currentCount must be between 0 and capacity");
        }
        this.capacity = capacity;
        this.currentCount = currentCount;
    }

    public String getAreaId() {
        return areaId;
    }

    public String getName() {
        return name;
    }

    public AreaType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void enterMember() {
        if (isAtCapacity()) {
            throw new IllegalStateException("area capacity reached for areaId=" + areaId);
        }
        currentCount++;
    }

    public void exitMember() {
        if (currentCount == 0) {
            throw new IllegalStateException("currentCount cannot be negative for areaId=" + areaId);
        }
        currentCount--;
    }

    public void updateCapacity(int newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("newCapacity must be > 0");
        }
        if (newCapacity < currentCount) {
            throw new IllegalArgumentException("newCapacity cannot be lower than currentCount");
        }
        capacity = newCapacity;
    }

    public boolean isAtCapacity() {
        return currentCount >= capacity;
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}

