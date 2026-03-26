package com.smartgym.areaservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "gym_areas")
public class GymArea {

    @Id
    private String id;

    private String name;
    private AreaType areaType;
    private Integer capacity;
    private Integer currentCount;

    public GymArea() {
    }

    public GymArea(String id, String name, AreaType areaType, Integer capacity, Integer currentCount) {
        this.id = id;
        this.name = name;
        this.areaType = areaType;
        this.capacity = capacity;
        this.currentCount = currentCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public void incrementCount() {
        if (this.currentCount == null) {
            this.currentCount = 0;
        }
        if (this.capacity != null && this.currentCount >= this.capacity) {
            throw new IllegalStateException("Area capacity exceeded");
        }
        this.currentCount++;
    }

    public void decrementCount() {
        if (this.currentCount == null || this.currentCount <= 0) {
            throw new IllegalStateException("Area count cannot be negative");
        }
        this.currentCount--;
    }

    public void updateCapacity(Integer newCapacity) {
        if (newCapacity == null || newCapacity < 0) {
            throw new IllegalArgumentException("Capacity must be greater than or equal to zero");
        }
        if (this.currentCount != null && newCapacity < this.currentCount) {
            throw new IllegalArgumentException("New capacity cannot be lower than current count");
        }
        this.capacity = newCapacity;
    }

    @Override
    public String toString() {
        return "GymArea{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", areaType=" + areaType +
                ", capacity=" + capacity +
                ", currentCount=" + currentCount +
                '}';
    }
}