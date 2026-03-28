package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceSnapshot {

    @JsonProperty("id")
    private String id;

    @JsonProperty("date")
    private String date;

    @JsonProperty("gymCount")
    private Integer gymCount;

    @JsonProperty("totalEntries")
    private Integer totalEntries;

    @JsonProperty("totalExits")
    private Integer totalExits;

    public AttendanceSnapshot() {
    }

    public AttendanceSnapshot(String id, String date, Integer gymCount, Integer totalEntries, Integer totalExits) {
        this.id = id;
        this.date = date;
        this.gymCount = gymCount;
        this.totalEntries = totalEntries;
        this.totalExits = totalExits;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public Integer getGymCount() {
        return gymCount;
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

    public void setGymCount(Integer gymCount) {
        this.gymCount = gymCount;
    }

    public void setTotalEntries(Integer totalEntries) {
        this.totalEntries = totalEntries;
    }

    public void setTotalExits(Integer totalExits) {
        this.totalExits = totalExits;
    }

    @Override
    public String toString() {
        return "AttendanceSnapshot{" +
                "id='" + id + '\'' +
                ", date='" + date + '\'' +
                ", gymCount=" + gymCount +
                ", totalEntries=" + totalEntries +
                ", totalExits=" + totalExits +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AttendanceSnapshot that = (AttendanceSnapshot) o;
        return Objects.equals(id, that.id) && Objects.equals(date, that.date) && Objects.equals(gymCount, that.gymCount) && Objects.equals(totalEntries, that.totalEntries) && Objects.equals(totalExits, that.totalExits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, gymCount, totalEntries, totalExits);
    }
}