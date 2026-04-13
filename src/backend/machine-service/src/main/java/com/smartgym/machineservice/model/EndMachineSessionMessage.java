package com.smartgym.machineservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndMachineSessionMessage {

    @JsonProperty("machineId")
    private String machineId;

    public EndMachineSessionMessage() {}

    public EndMachineSessionMessage(String machineId) {
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
        return "EndMachineSessionMessage{" + "machineId='" + machineId + '\'' + '}';
    }
}
