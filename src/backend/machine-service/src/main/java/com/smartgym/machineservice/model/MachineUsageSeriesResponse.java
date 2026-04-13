package com.smartgym.machineservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachineUsageSeriesResponse {

    @JsonProperty("meta")
    private final Meta meta;

    @JsonProperty("filters")
    private final Filters filters;

    @JsonProperty("series")
    private final List<Point> series;

    public MachineUsageSeriesResponse(Meta meta, Filters filters, List<Point> series) {
        this.meta = meta;
        this.filters = filters;
        this.series = series == null ? List.of() : List.copyOf(series);
    }

    public Meta getMeta() {
        return meta;
    }

    public Filters getFilters() {
        return filters;
    }

    public List<Point> getSeries() {
        return series;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        @JsonProperty("granularity")
        private final String granularity;

        public Meta(String granularity) {
            this.granularity = granularity;
        }

        public String getGranularity() {
            return granularity;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filters {
        @JsonProperty("from")
        private final String from;

        @JsonProperty("to")
        private final String to;

        @JsonProperty("areaId")
        private final String areaId;

        @JsonProperty("machineId")
        private final String machineId;

        public Filters(String from, String to, String areaId, String machineId) {
            this.from = from;
            this.to = to;
            this.areaId = areaId;
            this.machineId = machineId;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getAreaId() {
            return areaId;
        }

        public String getMachineId() {
            return machineId;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Point {
        @JsonProperty("period")
        private final String period;

        @JsonProperty("sessions")
        private final List<SessionItem> sessions;

        public Point(String period, List<SessionItem> sessions) {
            this.period = period;
            this.sessions = sessions == null ? List.of() : List.copyOf(sessions);
        }

        public String getPeriod() {
            return period;
        }

        public List<SessionItem> getSessions() {
            return sessions;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SessionItem {
        @JsonProperty("machineId")
        private final String machineId;

        @JsonProperty("areaId")
        private final String areaId;

        @JsonProperty("startTime")
        private final String startTime;

        @JsonProperty("endTime")
        private final String endTime;

        @JsonProperty("durationSeconds")
        private final long durationSeconds;

        @JsonProperty("badgeId")
        private final String badgeId;

        public SessionItem(
                String machineId,
                String areaId,
                String startTime,
                String endTime,
                long durationSeconds,
                String badgeId) {
            this.machineId = machineId;
            this.areaId = areaId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationSeconds = durationSeconds;
            this.badgeId = badgeId;
        }

        public String getMachineId() {
            return machineId;
        }

        public String getAreaId() {
            return areaId;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public long getDurationSeconds() {
            return durationSeconds;
        }

        public String getBadgeId() {
            return badgeId;
        }
    }
}
