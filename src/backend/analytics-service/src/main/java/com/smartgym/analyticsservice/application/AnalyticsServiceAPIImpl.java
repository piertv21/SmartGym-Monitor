package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AttendanceSeriesPoint;
import com.smartgym.analyticsservice.model.AttendanceSeriesResponse;
import com.smartgym.analyticsservice.model.GymSessionDurationStat;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AnalyticsServiceAPIImpl implements AnalyticsServiceAPI {

    private static final ZoneId ANALYTICS_ZONE = ZoneId.of("Europe/Rome");

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsServiceAPIImpl(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public CompletableFuture<Void> ingestEvent(JsonObject event) {
        if (event == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("event cannot be null"));
        }

        String eventType = event.getString("eventType");
        JsonObject payload = event.getJsonObject("payload");

        if (isBlank(eventType) || payload == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("eventType and payload are required"));
        }

        validatePayload(eventType, payload);
        JsonObject normalizedEvent = new JsonObject()
                .put("eventType", eventType.trim().toUpperCase())
                .put("payload", payload)
                .put("timestamp", extractTimestamp(payload));
        return analyticsRepository.saveEvent(normalizedEvent);
    }

    @Override
    public CompletableFuture<List<AttendanceSnapshot>> getAllAttendanceStats() {
        return analyticsRepository.findEventsByType("GYM_ACCESS")
                .thenApply(events -> groupByDate(events).entrySet().stream()
                        .map(entry -> toAttendanceSnapshot(entry.getKey(), entry.getValue()))
                        .sorted(Comparator.comparing(AttendanceSnapshot::getDate))
                        .toList());
    }

    @Override
    public CompletableFuture<AttendanceSeriesResponse> getAttendanceSeries(String from, String to, String granularity, String areaId) {
        LocalDate fromDate = parseLocalDate(from, "from");
        LocalDate toDate = parseLocalDate(to, "to");
        if (fromDate.isAfter(toDate)) {
            throw new IllegalStateException("from cannot be after to");
        }

        String normalizedGranularity = normalizeGranularity(granularity);
        String normalizedAreaId = normalizeOptional(areaId);
        String scope = normalizedAreaId == null ? "global" : "area";
        String eventType = normalizedAreaId == null ? "GYM_ACCESS" : "AREA_ACCESS";

        return analyticsRepository.findEventsByTypeAndDateRange(eventType, fromDate.toString(), toDate.toString())
                .thenApply(events -> toAttendanceSeries(
                        scope,
                        normalizedGranularity,
                        fromDate,
                        toDate,
                        normalizedAreaId,
                        events
                ));
    }

    @Override
    public CompletableFuture<GymSessionDurationStat> getGymSessionDurationByDate(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("GYM_ACCESS", normalizedDate)
                .thenApply(events -> toGymSessionDurationStat("day", normalizedDate, events));
    }

    private AttendanceSnapshot toAttendanceSnapshot(String date, List<JsonObject> events) {
        int entries = 0;
        int exits = 0;
        for (JsonObject event : events) {
            String accessType = normalizeUpper(event.getJsonObject("payload", new JsonObject()).getString("accessType"));
            if ("ENTRY".equals(accessType)) {
                entries += 1;
            } else if ("EXIT".equals(accessType)) {
                exits += 1;
            }
        }

        return new AttendanceSnapshot(
                "attendance-" + date,
                date,
                Math.max(entries - exits, 0),
                entries,
                exits
        );
    }

    private AttendanceSeriesResponse toAttendanceSeries(
            String scope,
            String granularity,
            LocalDate from,
            LocalDate to,
            String areaId,
            List<JsonObject> events
    ) {
        Map<String, int[]> buckets = initializeBuckets(from, to, granularity);
        for (JsonObject event : events) {
            JsonObject payload = event.getJsonObject("payload", new JsonObject());
            if ("area".equals(scope)) {
                String eventAreaId = normalizeString(payload.getString("areaId"));
                if (!areaId.equals(eventAreaId)) {
                    continue;
                }
            }

            String eventDate = event.getString("eventDate");
            if (isBlank(eventDate)) {
                continue;
            }

            String period;
            if ("monthly".equals(granularity)) {
                if (eventDate.length() < 7) {
                    continue;
                }
                period = eventDate.substring(0, 7);
            } else {
                period = eventDate;
            }

            int[] counter = buckets.get(period);
            if (counter == null) {
                continue;
            }

            String signal = "area".equals(scope)
                    ? normalizeUpper(payload.getString("direction"))
                    : normalizeUpper(payload.getString("accessType"));
            if ("ENTRY".equals(signal) || "IN".equals(signal)) {
                counter[0] += 1;
            } else if ("EXIT".equals(signal) || "OUT".equals(signal)) {
                counter[1] += 1;
            }
        }

        List<AttendanceSeriesPoint> series = buckets.entrySet().stream()
                .map(entry -> {
                    int totalEntries = entry.getValue()[0];
                    int totalExits = entry.getValue()[1];
                    return new AttendanceSeriesPoint(
                            entry.getKey(),
                            totalEntries,
                            totalEntries,
                            totalExits
                    );
                })
                .toList();

        return new AttendanceSeriesResponse(
                new AttendanceSeriesResponse.Meta(scope, granularity, ANALYTICS_ZONE.getId()),
                new AttendanceSeriesResponse.Filters(from.toString(), to.toString(), areaId),
                series
        );
    }

    private Map<String, int[]> initializeBuckets(LocalDate from, LocalDate to, String granularity) {
        Map<String, int[]> buckets = new LinkedHashMap<>();
        if ("monthly".equals(granularity)) {
            YearMonth cursor = YearMonth.from(from);
            YearMonth end = YearMonth.from(to);
            while (!cursor.isAfter(end)) {
                buckets.put(cursor.toString(), new int[]{0, 0});
                cursor = cursor.plusMonths(1);
            }
            return buckets;
        }

        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            buckets.put(cursor.toString(), new int[]{0, 0});
            cursor = cursor.plusDays(1);
        }
        return buckets;
    }

    private GymSessionDurationStat toGymSessionDurationStat(String periodType, String periodValue, List<JsonObject> events) {
        List<Double> durations = computeSessionDurations(events);
        double avg = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double max = durations.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        return new GymSessionDurationStat(
                "gym-duration-" + periodType + "-" + periodValue,
                periodType,
                periodValue,
                avg,
                max,
                durations.size()
        );
    }

    private List<Double> computeSessionDurations(List<JsonObject> events) {
        List<JsonObject> ordered = sortByTimestamp(events);
        Map<String, LocalDateTime> starts = new HashMap<>();
        List<Double> durations = new ArrayList<>();

        for (JsonObject event : ordered) {
            JsonObject payload = event.getJsonObject("payload", new JsonObject());
            String badgeId = normalizeString(payload.getString("badgeId"));
            if (isBlank(badgeId)) {
                continue;
            }

            String state = normalizeUpper(payload.getString("accessType"));
            LocalDateTime timestamp = parseTimestamp(extractTimestamp(payload));

            if ("ENTRY".equals(state)) {
                starts.put(badgeId, timestamp);
            } else if ("EXIT".equals(state)) {
                LocalDateTime startedAt = starts.remove(badgeId);
                if (startedAt != null && !timestamp.isBefore(startedAt)) {
                    durations.add(Duration.between(startedAt, timestamp).toMillis() / 60000.0);
                }
            }
        }
        return durations;
    }

    private Map<String, List<JsonObject>> groupByDate(List<JsonObject> events) {
        return events.stream().collect(Collectors.groupingBy(event -> event.getString("eventDate")));
    }

    private List<JsonObject> sortByTimestamp(List<JsonObject> events) {
        return events.stream()
                .sorted(Comparator.comparing(event -> parseTimestamp(event.getString("timestamp"))))
                .toList();
    }

    private void validatePayload(String eventType, JsonObject payload) {
        String normalizedType = eventType.trim().toUpperCase();
        switch (normalizedType) {
            case "GYM_ACCESS" -> {
                String accessType = normalizeUpper(payload.getString("accessType"));
                if (!"ENTRY".equals(accessType) && !"EXIT".equals(accessType)) {
                    throw new IllegalArgumentException("Gym access payload requires accessType ENTRY or EXIT");
                }
            }
            case "MACHINE_USAGE" -> {
                String machineId = normalizeString(payload.getString("machineId"));
                String usageState = normalizeUpper(payload.getString("usageState"));
                if (isBlank(machineId) || (!"STARTED".equals(usageState) && !"STOPPED".equals(usageState))) {
                    throw new IllegalArgumentException("Machine usage payload requires machineId and usageState STARTED or STOPPED");
                }
            }
            case "AREA_ACCESS" -> {
                String areaId = normalizeString(payload.getString("areaId"));
                String direction = normalizeUpper(payload.getString("direction"));
                if (isBlank(areaId) || (!"IN".equals(direction) && !"OUT".equals(direction))) {
                    throw new IllegalArgumentException("Area access payload requires areaId and direction IN or OUT");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported eventType: " + eventType);
        }

        extractTimestamp(payload);
    }

    private LocalDate parseLocalDate(String date, String fieldName) {
        if (isBlank(date)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(fieldName + " must be in format yyyy-MM-dd", ex);
        }
    }

    private String normalizeGranularity(String granularity) {
        if (isBlank(granularity)) {
            return "daily";
        }
        String normalized = granularity.trim().toLowerCase();
        if (!"daily".equals(normalized) && !"monthly".equals(normalized)) {
            throw new IllegalArgumentException("granularity must be daily or monthly");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeDate(String date) {
        if (isBlank(date)) {
            throw new IllegalArgumentException("date cannot be null or empty");
        }
        return LocalDate.parse(date.trim()).toString();
    }

    private String normalizeString(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }


    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(timestamp), ANALYTICS_ZONE);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(timestamp);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid timestamp format: " + timestamp, ex);
            }
        }
    }

    private String extractTimestamp(JsonObject payload) {
        String timeStamp = payload.getString("timeStamp");
        if (!isBlank(timeStamp)) {
            return timeStamp;
        }
        String timestamp = payload.getString("timestamp");
        if (isBlank(timestamp)) {
            throw new IllegalArgumentException("payload requires timeStamp or timestamp");
        }
        return timestamp;
    }
}