package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AreaSessionDurationStat {

    @JsonProperty("id")
    private String id;

    @JsonProperty("periodType")
    private String periodType;

    @JsonProperty("periodValue")
    private String periodValue;

    @JsonProperty("areaId")
    private String areaId;

    @JsonProperty("averageDurationMinutes")
    private Double averageDurationMinutes;

    @JsonProperty("maxDurationMinutes")
    private Double maxDurationMinutes;

    @JsonProperty("sessionCount")
    private Integer sessionCount;

    public AreaSessionDurationStat() {
    }

    public AreaSessionDurationStat(String id,
                                   String periodType,
                                   String periodValue,
                                   String areaId,
                                   Double averageDurationMinutes,
                                   Double maxDurationMinutes,
                                   Integer sessionCount) {
        this.id = id;
        this.periodType = periodType;
        this.periodValue = periodValue;
        this.areaId = areaId;
        this.averageDurationMinutes = averageDurationMinutes;
        this.maxDurationMinutes = maxDurationMinutes;
        this.sessionCount = sessionCount;
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

    public String getAreaId() {
        return areaId;
    }

    public Double getAverageDurationMinutes() {
        return averageDurationMinutes;
    }

    public Double getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public Integer getSessionCount() {
        return sessionCount;
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

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public void setAverageDurationMinutes(Double averageDurationMinutes) {
        this.averageDurationMinutes = averageDurationMinutes;
    }

    public void setMaxDurationMinutes(Double maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public void setSessionCount(Integer sessionCount) {
        this.sessionCount = sessionCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AreaSessionDurationStat that = (AreaSessionDurationStat) o;
        return Objects.equals(id, that.id)
                && Objects.equals(periodType, that.periodType)
                && Objects.equals(periodValue, that.periodValue)
                && Objects.equals(areaId, that.areaId)
                && Objects.equals(averageDurationMinutes, that.averageDurationMinutes)
                && Objects.equals(maxDurationMinutes, that.maxDurationMinutes)
                && Objects.equals(sessionCount, that.sessionCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, periodType, periodValue, areaId, averageDurationMinutes, maxDurationMinutes, sessionCount);
    }
}

