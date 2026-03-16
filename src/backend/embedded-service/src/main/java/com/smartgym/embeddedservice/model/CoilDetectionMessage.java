package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoilDetectionMessage {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("timeStamp")
    private String timestamp;

    @JsonProperty("coilAt") //"entry" or "exit"
    private String coilAt;

    @JsonProperty("status") //"entry" or "exit"
    private String status;

    public CoilDetectionMessage() {
    }

    public CoilDetectionMessage(String deviceId, String timestamp, String coilAt, String status) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.coilAt = coilAt;
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getCoilAt() {
        return coilAt;
    }

    public String getStatus() {
        return status;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setCoilAt(String coilAt) {
        this.coilAt = coilAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "CoilDetectionMessage{" +
                "deviceId='" + deviceId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", coilAt='" + coilAt + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}