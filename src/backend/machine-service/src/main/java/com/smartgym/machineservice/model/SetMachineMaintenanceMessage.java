package com.smartgym.machineservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SetMachineMaintenanceMessage {

    @JsonProperty("machineId")
    private String machineId;

    @JsonProperty("active")
    private Boolean active;

    public SetMachineMaintenanceMessage() {}

    public SetMachineMaintenanceMessage(String machineId) {
        this(machineId, true);
    }

    public SetMachineMaintenanceMessage(String machineId, Boolean active) {
        this.machineId = machineId;
        this.active = active;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "SetMachineMaintenanceMessage{"
                + "machineId='"
                + machineId
                + '\''
                + ", active="
                + active
                + '}';
    }
}
