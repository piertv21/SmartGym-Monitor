package com.smartgym.machineservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StartMachineSessionMessage {

    @JsonProperty("machineId")
    private String machineId;

    @JsonProperty("badgeId")
    private String badgeId;

    public StartMachineSessionMessage() {
    }

    public StartMachineSessionMessage(String machineId, String badgeId) {
        this.machineId = machineId;
        this.badgeId = badgeId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    @Override
    public String toString() {
        return "StartMachineSessionMessage{" +
                "machineId='" + machineId + '\'' +
                ", badgeId='" + badgeId + '\'' +
                '}';
    }
}