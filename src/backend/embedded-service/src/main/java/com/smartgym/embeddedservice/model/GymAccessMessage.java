package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GymAccessMessage {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("timeStamp")
    private String timestamp;

    @JsonProperty("badgeId")
    private String badgeId;

    @JsonProperty("accessType") // "ENTRY" or "EXIT"
    private String accessType;

    public GymAccessMessage() {}

    public GymAccessMessage(String deviceId, String timestamp, String badgeId, String accessType) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.badgeId = badgeId;
        this.accessType = accessType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    @Override
    public String toString() {
        return "GymAccessMessage{"
                + "deviceId='"
                + deviceId
                + '\''
                + ", timestamp='"
                + timestamp
                + '\''
                + ", badgeId='"
                + badgeId
                + '\''
                + ", accessType='"
                + accessType
                + '\''
                + '}';
    }
}
