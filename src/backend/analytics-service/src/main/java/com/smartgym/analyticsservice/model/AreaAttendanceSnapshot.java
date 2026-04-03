package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AreaAttendanceSnapshot {

    @JsonProperty("id")
    private String id;

    @JsonProperty("date")
    private String date;

    @JsonProperty("areaId")
    private String areaId;

    @JsonProperty("currentCount")
    private Integer currentCount;

    @JsonProperty("totalEntries")
    private Integer totalEntries;

    @JsonProperty("totalExits")
    private Integer totalExits;

    public AreaAttendanceSnapshot() {
    }

    public AreaAttendanceSnapshot(String id,
                                  String date,
                                  String areaId,
                                  Integer currentCount,
                                  Integer totalEntries,
                                  Integer totalExits) {
        this.id = id;
        this.date = date;
        this.areaId = areaId;
        this.currentCount = currentCount;
        this.totalEntries = totalEntries;
        this.totalExits = totalExits;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getAreaId() {
        return areaId;
    }

    public Integer getCurrentCount() {
        return currentCount;
    }

    public Integer getTotalEntries() {
        return totalEntries;
    }

    public Integer getTotalExits() {
        return totalExits;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public void setTotalEntries(Integer totalEntries) {
        this.totalEntries = totalEntries;
    }

    public void setTotalExits(Integer totalExits) {
        this.totalExits = totalExits;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AreaAttendanceSnapshot that = (AreaAttendanceSnapshot) o;
        return Objects.equals(id, that.id)
                && Objects.equals(date, that.date)
                && Objects.equals(areaId, that.areaId)
                && Objects.equals(currentCount, that.currentCount)
                && Objects.equals(totalEntries, that.totalEntries)
                && Objects.equals(totalExits, that.totalExits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, areaId, currentCount, totalEntries, totalExits);
    }
}

