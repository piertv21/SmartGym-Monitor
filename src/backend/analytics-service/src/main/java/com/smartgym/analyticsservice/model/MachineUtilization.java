package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachineUtilization {

    @JsonProperty("id")
    private String id;

    @JsonProperty("machineId")
    private String machineId;

    @JsonProperty("date")
    private String date;

    @JsonProperty("usageCount")
    private Integer usageCount;

    @JsonProperty("totalUsageMinutes")
    private Double totalUsageMinutes;

    @JsonProperty("utilizationRate")
    private Double utilizationRate;

    public MachineUtilization() {
    }

    public MachineUtilization(String id,
                              String machineId,
                              String date,
                              Integer usageCount,
                              Double totalUsageMinutes,
                              Double utilizationRate) {
        this.id = id;
        this.machineId = machineId;
        this.date = date;
        this.usageCount = usageCount;
        this.totalUsageMinutes = totalUsageMinutes;
        this.utilizationRate = utilizationRate;
    }

    public String getId() {
        return id;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getDate() {
        return date;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public Double getTotalUsageMinutes() {
        return totalUsageMinutes;
    }

    public Double getUtilizationRate() {
        return utilizationRate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public void setTotalUsageMinutes(Double totalUsageMinutes) {
        this.totalUsageMinutes = totalUsageMinutes;
    }

    public void setUtilizationRate(Double utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    @Override
    public String toString() {
        return "MachineUtilization{" +
                "id='" + id + '\'' +
                ", machineId='" + machineId + '\'' +
                ", date='" + date + '\'' +
                ", usageCount=" + usageCount +
                ", totalUsageMinutes=" + totalUsageMinutes +
                ", utilizationRate=" + utilizationRate +
                '}';
    }
}