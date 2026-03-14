package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlateDetectionMessage {

    @JsonProperty("timeStamp")
    private String timeStamp;

    @JsonProperty("date")
    private String date;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("licensePlate")
    private String licensePlate;

    @JsonProperty("cameraAt")
    private String cameraAt;

    public PlateDetectionMessage() {}

    public PlateDetectionMessage(String timeStamp, String date, String deviceId, String licensePlate) {
        this.timeStamp = timeStamp;
        this.date = date;
        this.deviceId = deviceId;
        this.licensePlate = licensePlate;
    }

    public String getTimeStamp() { return timeStamp; }
    public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    @Override
    public String toString() {
        return "PlateDetectionMessage{" +
                "timeStamp='" + timeStamp + '\'' +
                ", date='" + date + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", licensePlate='" + licensePlate + '\'' +
                ", cameraAt='" + cameraAt + '\'' +
                '}';
    }

    public String getCameraAt() {
        return cameraAt;
    }

    public void setCameraAt(String cameraAt) {
        this.cameraAt = cameraAt;
    }
}
