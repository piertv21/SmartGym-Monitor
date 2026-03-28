package com.smartgym.machineservice.model;

public class SetMachineMaintenanceMessage {

    private String machineId;

    public SetMachineMaintenanceMessage() {
    }

    public SetMachineMaintenanceMessage(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
}