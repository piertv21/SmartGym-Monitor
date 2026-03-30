package com.smartgym.machineservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigureMachineMessage {

    @JsonProperty("machineId")
    private String machineId;

    @JsonProperty("areaId")
    private String areaId;

    @JsonProperty("sensor")
    private String sensor;

    public ConfigureMachineMessage() {
    }

    public ConfigureMachineMessage(String machineId, String areaId) {
        this.machineId = machineId;
        this.areaId = areaId;
    }

    public ConfigureMachineMessage(String machineId, String areaId, String sensor) {
        this.machineId = machineId;
        this.areaId = areaId;
        this.sensor = sensor;
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

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    @Override
    public String toString() {
        return "ConfigureMachineMessage{" +
                "machineId='" + machineId + '\'' +
                ", areaId='" + areaId + '\'' +
                ", sensor='" + sensor + '\'' +
                '}';
    }
}