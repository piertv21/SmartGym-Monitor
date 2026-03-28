package com.smartgym.machineservice.model;

public class StartMachineSessionMessage {

    private String machineId;
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
}