package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyboardPlateInsertionMessage {

    @JsonProperty("plate")
    private String plate;

    @JsonProperty("deviceId")
    private String deviceId;

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "KeyboardPlateInsertionMessage{" +
                "plate='" + plate + '\'' +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
}