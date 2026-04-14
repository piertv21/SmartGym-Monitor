package com.smartgym.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceSeriesResponse {

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("filters")
    private Filters filters;

    @JsonProperty("series")
    private List<AttendanceSeriesPoint> series;

    public AttendanceSeriesResponse() {}

    public AttendanceSeriesResponse(
            Meta meta, Filters filters, List<AttendanceSeriesPoint> series) {
        this.meta = meta;
        this.filters = filters;
        this.series = series;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    public List<AttendanceSeriesPoint> getSeries() {
        return series;
    }

    public void setSeries(List<AttendanceSeriesPoint> series) {
        this.series = series;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("granularity")
        private String granularity;

        @JsonProperty("timezone")
        private String timezone;

        public Meta() {}

        public Meta(String scope, String granularity, String timezone) {
            this.scope = scope;
            this.granularity = granularity;
            this.timezone = timezone;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getGranularity() {
            return granularity;
        }

        public void setGranularity(String granularity) {
            this.granularity = granularity;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filters {

        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;

        @JsonProperty("areaId")
        private String areaId;

        public Filters() {}

        public Filters(String from, String to, String areaId) {
            this.from = from;
            this.to = to;
            this.areaId = areaId;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getAreaId() {
            return areaId;
        }

        public void setAreaId(String areaId) {
            this.areaId = areaId;
        }
    }
}
