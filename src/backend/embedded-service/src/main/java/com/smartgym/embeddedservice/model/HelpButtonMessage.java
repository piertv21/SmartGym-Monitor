package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HelpButtonMessage {

    @JsonProperty("timeStamp")
    private String timeStamp;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("value")
    private String value;  // "pressed" or "not_pressed"


    public HelpButtonMessage() {}

    public HelpButtonMessage(String timeStamp, String deviceId, String value) {
        this.timeStamp = timeStamp;
        this.deviceId = deviceId;
        this.value = value;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "HelpButtonMessage{" +
                "timeStamp='" + timeStamp + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
