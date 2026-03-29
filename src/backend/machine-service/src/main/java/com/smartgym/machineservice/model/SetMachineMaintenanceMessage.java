package com.smartgym.machineservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SetMachineMaintenanceMessage {

    @JsonProperty("machineId")
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

    @Override
    public String toString() {
        return "SetMachineMaintenanceMessage{" +
                "machineId='" + machineId + '\'' +
                '}';
    }
}