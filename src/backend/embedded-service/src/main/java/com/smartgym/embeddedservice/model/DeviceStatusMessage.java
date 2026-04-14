package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceStatusMessage {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("timeStamp")
    private String timestamp;

    @JsonProperty("deviceType") // "TURNSTILE", "RFID_READER", "DOOR_READER", "PROXIMITY_SENSOR"
    private String deviceType;

    @JsonProperty("online")
    private Boolean online;

    @JsonProperty("statusDetail")
    private String statusDetail;

    public DeviceStatusMessage() {}

    public DeviceStatusMessage(
            String deviceId,
            String timestamp,
            String deviceType,
            Boolean online,
            String statusDetail) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.deviceType = deviceType;
        this.online = online;
        this.statusDetail = statusDetail;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Boolean getOnline() {
        return online;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail;
    }

    @Override
    public String toString() {
        return "DeviceStatusMessage{"
                + "deviceId='"
                + deviceId
                + '\''
                + ", timestamp='"
                + timestamp
                + '\''
                + ", deviceType='"
                + deviceType
                + '\''
                + ", online="
                + online
                + ", statusDetail='"
                + statusDetail
                + '\''
                + '}';
    }
}
