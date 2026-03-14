package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusMessage {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("status")
    private String status;

    public StatusMessage() {}

    public StatusMessage(String deviceId, String status) {
        this.deviceId = deviceId;
        this.status = status;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StatusMessage{" +
                "deviceId='" + deviceId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
