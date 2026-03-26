package com.smartgym.areaservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAreaCapacityMessage {

    @JsonProperty("areaId")
    private String areaId;

    @JsonProperty("capacity")
    private Integer capacity;

    public UpdateAreaCapacityMessage() {
    }

    public UpdateAreaCapacityMessage(String areaId, Integer capacity) {
        this.areaId = areaId;
        this.capacity = capacity;
    }

    public String getAreaId() {
        return areaId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "UpdateAreaCapacityMessage{" +
                "areaId='" + areaId + '\'' +
                ", capacity=" + capacity +
                '}';
    }
}