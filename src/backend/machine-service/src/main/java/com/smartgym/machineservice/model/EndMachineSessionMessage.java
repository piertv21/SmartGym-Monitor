package com.smartgym.machineservice.model;

public class EndMachineSessionMessage {

    private String machineId;

    public EndMachineSessionMessage() {
    }

    public EndMachineSessionMessage(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
}