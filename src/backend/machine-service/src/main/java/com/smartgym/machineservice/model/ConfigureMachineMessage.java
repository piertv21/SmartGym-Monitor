package com.smartgym.machineservice.model;

public class ConfigureMachineMessage {

    private String machineId;
    private String areaId;

    public ConfigureMachineMessage() {
    }

    public ConfigureMachineMessage(String machineId, String areaId) {
        this.machineId = machineId;
        this.areaId = areaId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }
}