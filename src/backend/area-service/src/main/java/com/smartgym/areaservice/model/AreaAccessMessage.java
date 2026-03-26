package com.smartgym.areaservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AreaAccessMessage {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("timeStamp")
    private String timeStamp;

    @JsonProperty("badgeId")
    private String badgeId;

    @JsonProperty("areaId")
    private String areaId;

    @JsonProperty("direction") // "IN" or "OUT"
    private String direction;

    public AreaAccessMessage() {
    }

    public AreaAccessMessage(String deviceId, String timeStamp, String badgeId, String areaId, String direction) {
        this.deviceId = deviceId;
        this.timeStamp = timeStamp;
        this.badgeId = badgeId;
        this.areaId = areaId;
        this.direction = direction;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public String getAreaId() {
        return areaId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public boolean isEntry() {
        return "IN".equalsIgnoreCase(direction);
    }

    public boolean isExit() {
        return "OUT".equalsIgnoreCase(direction);
    }

    @Override
    public String toString() {
        return "AreaAccessMessage{" +
                "deviceId='" + deviceId + '\'' +
                ", timeStamp='" + timeStamp + '\'' +
                ", badgeId='" + badgeId + '\'' +
                ", areaId='" + areaId + '\'' +
                ", direction='" + direction + '\'' +
                '}';
    }
}