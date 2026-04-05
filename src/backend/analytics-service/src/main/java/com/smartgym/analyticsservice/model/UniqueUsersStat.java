package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UniqueUsersStat {

    @JsonProperty("id")
    private String id;

    @JsonProperty("periodType")
    private String periodType;

    @JsonProperty("periodValue")
    private String periodValue;

    @JsonProperty("uniqueUsers")
    private Integer uniqueUsers;

    public UniqueUsersStat() {
    }

    public UniqueUsersStat(String id, String periodType, String periodValue, Integer uniqueUsers) {
        this.id = id;
        this.periodType = periodType;
        this.periodValue = periodValue;
        this.uniqueUsers = uniqueUsers;
    }

    public String getId() {
        return id;
    }

    public String getPeriodType() {
        return periodType;
    }

    public String getPeriodValue() {
        return periodValue;
    }

    public Integer getUniqueUsers() {
        return uniqueUsers;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public void setPeriodValue(String periodValue) {
        this.periodValue = periodValue;
    }

    public void setUniqueUsers(Integer uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UniqueUsersStat that = (UniqueUsersStat) o;
        return Objects.equals(id, that.id)
                && Objects.equals(periodType, that.periodType)
                && Objects.equals(periodValue, that.periodValue)
                && Objects.equals(uniqueUsers, that.uniqueUsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, periodType, periodValue, uniqueUsers);
    }
}

