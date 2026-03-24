package com.smartgym.embeddedservice.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BadgeScannedMessage {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("timeStamp")
    private String timestamp;

    @JsonProperty("badgeId")
    private String badgeId;

    @JsonProperty("scanType") // "gymEntrance", "gymExit", "areaAccess"
    private String scanType;

    @JsonProperty("areaId")
    private String areaId;

    public BadgeScannedMessage() {
    }

    public BadgeScannedMessage(String deviceId, String timestamp, String badgeId, String scanType, String areaId) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.badgeId = badgeId;
        this.scanType = scanType;
        this.areaId = areaId;
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

    public String getScanType() {
        return scanType;
    }

    public String getAreaId() {
        return areaId;
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

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    @Override
    public String toString() {
        return "BadgeScannedMessage{" +
                "deviceId='" + deviceId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", badgeId='" + badgeId + '\'' +
                ", scanType='" + scanType + '\'' +
                ", areaId='" + areaId + '\'' +
                '}';
    }
}