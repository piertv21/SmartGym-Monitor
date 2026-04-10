package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceSeriesPoint {

    @JsonProperty("period")
    private String period;

    @JsonProperty("currentCount")
    private Integer currentCount;

    @JsonProperty("totalEntries")
    private Integer totalEntries;

    @JsonProperty("totalExits")
    private Integer totalExits;

    public AttendanceSeriesPoint() {
    }

    public AttendanceSeriesPoint(String period, Integer currentCount, Integer totalEntries, Integer totalExits) {
        this.period = period;
        this.currentCount = currentCount;
        this.totalEntries = totalEntries;
        this.totalExits = totalExits;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public Integer getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(Integer totalEntries) {
        this.totalEntries = totalEntries;
    }

    public Integer getTotalExits() {
        return totalExits;
    }

    public void setTotalExits(Integer totalExits) {
        this.totalExits = totalExits;
    }
}

