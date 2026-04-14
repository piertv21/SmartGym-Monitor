package com.smartgym.embeddedservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachineUsageMessage {

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("timeStamp")
    private String timestamp;

    @JsonProperty("machineId")
    private String machineId;

    @JsonProperty("badgeId")
    private String badgeId;

    @JsonProperty("usageState") // "STARTED" or "STOPPED"
    private String usageState;

    public MachineUsageMessage() {}

    public MachineUsageMessage(
            String deviceId,
            String timestamp,
            String machineId,
            String badgeId,
            String usageState) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.machineId = machineId;
        this.badgeId = badgeId;
        this.usageState = usageState;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public String getUsageState() {
        return usageState;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public void setUsageState(String usageState) {
        this.usageState = usageState;
    }

    @Override
    public String toString() {
        return "MachineUsageMessage{"
                + "deviceId='"
                + deviceId
                + '\''
                + ", timestamp='"
                + timestamp
                + '\''
                + ", machineId='"
                + machineId
                + '\''
                + ", badgeId='"
                + badgeId
                + '\''
                + ", usageState='"
                + usageState
                + '\''
                + '}';
    }
}
