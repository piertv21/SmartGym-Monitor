package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AreaPeakHourStat {

    @JsonProperty("id")
    private String id;

    @JsonProperty("date")
    private String date;

    @JsonProperty("areaId")
    private String areaId;

    @JsonProperty("hour")
    private Integer hour;

    @JsonProperty("attendanceCount")
    private Integer attendanceCount;

    public AreaPeakHourStat() {
    }

    public AreaPeakHourStat(String id, String date, String areaId, Integer hour, Integer attendanceCount) {
        this.id = id;
        this.date = date;
        this.areaId = areaId;
        this.hour = hour;
        this.attendanceCount = attendanceCount;
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

    public Integer getHour() {
        return hour;
    }

    public Integer getAttendanceCount() {
        return attendanceCount;
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

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public void setAttendanceCount(Integer attendanceCount) {
        this.attendanceCount = attendanceCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AreaPeakHourStat that = (AreaPeakHourStat) o;
        return Objects.equals(id, that.id)
                && Objects.equals(date, that.date)
                && Objects.equals(areaId, that.areaId)
                && Objects.equals(hour, that.hour)
                && Objects.equals(attendanceCount, that.attendanceCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, areaId, hour, attendanceCount);
    }
}

