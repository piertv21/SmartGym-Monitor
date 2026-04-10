package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AttendanceSeriesPoint;
import com.smartgym.analyticsservice.model.AttendanceSeriesResponse;
import com.smartgym.analyticsservice.model.AreaAttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaPeakHourStat;
import com.smartgym.analyticsservice.model.AreaSessionDurationStat;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.GymSessionDurationStat;
import com.smartgym.analyticsservice.model.PeakHourStat;
import com.smartgym.analyticsservice.model.UniqueUsersStat;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AnalyticsServiceAPIImpl implements AnalyticsServiceAPI {

    private static final double MINUTES_PER_HOUR = 60.0;
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
    public CompletableFuture<Optional<AttendanceSnapshot>> getAttendanceStats(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("GYM_ACCESS", normalizedDate)
                .thenApply(events -> {
                    if (events.isEmpty()) {
                        return Optional.empty();
                    }
                    return Optional.of(toAttendanceSnapshot(normalizedDate, events));
                });
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
    public CompletableFuture<List<MachineUtilization>> getMachineUtilization() {
        return analyticsRepository.findEventsByType("MACHINE_USAGE")
                .thenApply(this::toDailyMachineUtilization);
    }

    @Override
    public CompletableFuture<List<MachineUtilization>> getMachineUtilizationByDate(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("MACHINE_USAGE", normalizedDate)
                .thenApply(events -> toMachineUtilizationByPeriod(events, normalizedDate));
    }

    @Override
    public CompletableFuture<List<MachineUtilization>> getMachineUtilizationByMonth(String month) {
        String normalizedMonth = normalizeMonth(month);
        return analyticsRepository.findEventsByTypeAndMonth("MACHINE_USAGE", normalizedMonth)
                .thenApply(events -> toMachineUtilizationByPeriod(events, normalizedMonth));
    }

    @Override
    public CompletableFuture<UniqueUsersStat> getUniqueUsersByDate(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("GYM_ACCESS", normalizedDate)
                .thenApply(events -> toUniqueUsersStat("day", normalizedDate, events));
    }

    @Override
    public CompletableFuture<UniqueUsersStat> getUniqueUsersByMonth(String month) {
        String normalizedMonth = normalizeMonth(month);
        return analyticsRepository.findEventsByTypeAndMonth("GYM_ACCESS", normalizedMonth)
                .thenApply(events -> toUniqueUsersStat("month", normalizedMonth, events));
    }

    @Override
    public CompletableFuture<GymSessionDurationStat> getGymSessionDurationByDate(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("GYM_ACCESS", normalizedDate)
                .thenApply(events -> toGymSessionDurationStat("day", normalizedDate, events));
    }

    @Override
    public CompletableFuture<GymSessionDurationStat> getGymSessionDurationByMonth(String month) {
        String normalizedMonth = normalizeMonth(month);
        return analyticsRepository.findEventsByTypeAndMonth("GYM_ACCESS", normalizedMonth)
                .thenApply(events -> toGymSessionDurationStat("month", normalizedMonth, events));
    }

    @Override
    public CompletableFuture<AreaSessionDurationStat> getAreaSessionDurationByDate(String date, String areaId) {
        String normalizedDate = normalizeDate(date);
        String normalizedAreaId = normalizeNonBlank(areaId, "areaId");
        return analyticsRepository.findEventsByTypeAndDate("AREA_ACCESS", normalizedDate)
                .thenApply(events -> toAreaSessionDurationStat("day", normalizedDate, normalizedAreaId, events));
    }

    @Override
    public CompletableFuture<AreaSessionDurationStat> getAreaSessionDurationByMonth(String month, String areaId) {
        String normalizedMonth = normalizeMonth(month);
        String normalizedAreaId = normalizeNonBlank(areaId, "areaId");
        return analyticsRepository.findEventsByTypeAndMonth("AREA_ACCESS", normalizedMonth)
                .thenApply(events -> toAreaSessionDurationStat("month", normalizedMonth, normalizedAreaId, events));
    }

    @Override
    public CompletableFuture<List<PeakHourStat>> getPeakHours() {
        return analyticsRepository.findEventsByType("GYM_ACCESS")
                .thenApply(this::toPeakHours);
    }

    @Override
    public CompletableFuture<List<PeakHourStat>> getPeakHoursByDate(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("GYM_ACCESS", normalizedDate)
                .thenApply(events -> toPeakHoursForDate(normalizedDate, events));
    }

    @Override
    public CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendance() {
        return analyticsRepository.findEventsByType("AREA_ACCESS")
                .thenApply(this::toAreaAttendanceSnapshots);
    }

    @Override
    public CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendanceByDate(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("AREA_ACCESS", normalizedDate)
                .thenApply(events -> toAreaAttendanceByDate(normalizedDate, events));
    }

    @Override
    public CompletableFuture<Optional<AreaAttendanceSnapshot>> getAreaAttendanceByDateAndAreaId(String date, String areaId) {
        String normalizedDate = normalizeDate(date);
        String normalizedAreaId = normalizeNonBlank(areaId, "areaId");
        return analyticsRepository.findEventsByTypeAndDate("AREA_ACCESS", normalizedDate)
                .thenApply(events -> toAreaAttendanceByDate(normalizedDate, events).stream()
                        .filter(snapshot -> normalizedAreaId.equals(snapshot.getAreaId()))
                        .findFirst());
    }

    @Override
    public CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHours() {
        return analyticsRepository.findEventsByType("AREA_ACCESS")
                .thenApply(this::toAreaPeakHours);
    }

    @Override
    public CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDate(String date) {
        String normalizedDate = normalizeDate(date);
        return analyticsRepository.findEventsByTypeAndDate("AREA_ACCESS", normalizedDate)
                .thenApply(events -> toAreaPeakHoursByDate(normalizedDate, events));
    }

    @Override
    public CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDateAndAreaId(String date, String areaId) {
        String normalizedDate = normalizeDate(date);
        String normalizedAreaId = normalizeNonBlank(areaId, "areaId");
        return analyticsRepository.findEventsByTypeAndDate("AREA_ACCESS", normalizedDate)
                .thenApply(events -> toAreaPeakHoursByDate(normalizedDate, events).stream()
                        .filter(stat -> normalizedAreaId.equals(stat.getAreaId()))
                        .toList());
    }

    private List<MachineUtilization> toDailyMachineUtilization(List<JsonObject> events) {
        return groupByDate(events).entrySet().stream()
                .flatMap(entry -> toMachineUtilizationByPeriod(entry.getValue(), entry.getKey()).stream())
                .sorted(Comparator.comparing(MachineUtilization::getDate).thenComparing(MachineUtilization::getMachineId))
                .toList();
    }

    private List<MachineUtilization> toMachineUtilizationByPeriod(List<JsonObject> events, String periodValue) {
        Map<String, List<JsonObject>> byMachine = events.stream()
                .filter(event -> !isBlank(event.getJsonObject("payload", new JsonObject()).getString("machineId")))
                .collect(Collectors.groupingBy(event -> event.getJsonObject("payload").getString("machineId").trim()));

        List<MachineUtilization> result = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> entry : byMachine.entrySet()) {
            String machineId = entry.getKey();
            List<JsonObject> ordered = sortByTimestamp(entry.getValue());

            int starts = 0;
            double totalMinutes = 0.0;
            ArrayDeque<LocalDateTime> openSessions = new ArrayDeque<>();

            for (JsonObject event : ordered) {
                JsonObject payload = event.getJsonObject("payload", new JsonObject());
                String state = normalizeUpper(payload.getString("usageState"));
                LocalDateTime timestamp = parseTimestamp(extractTimestamp(payload));
                if ("STARTED".equals(state)) {
                    starts += 1;
                    openSessions.push(timestamp);
                } else if ("STOPPED".equals(state) && !openSessions.isEmpty()) {
                    LocalDateTime start = openSessions.pop();
                    if (!timestamp.isBefore(start)) {
                        totalMinutes += Duration.between(start, timestamp).toMillis() / 60000.0;
                    }
                }
            }

            result.add(new MachineUtilization(
                    "machine-" + machineId + "-" + periodValue,
                    machineId,
                    periodValue,
                    starts,
                    totalMinutes,
                    computeHourlyUtilizationRate(totalMinutes)
            ));
        }

        result.sort(Comparator.comparing(MachineUtilization::getMachineId));
        return result;
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

    private List<PeakHourStat> toPeakHours(List<JsonObject> events) {
        Map<String, List<JsonObject>> byDate = groupByDate(events);
        List<PeakHourStat> result = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> entry : byDate.entrySet()) {
            result.addAll(toPeakHoursForDate(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparing(PeakHourStat::getDate).thenComparing(PeakHourStat::getHour));
        return result;
    }

    private List<PeakHourStat> toPeakHoursForDate(String date, List<JsonObject> events) {
        Map<Integer, Integer> byHour = new HashMap<>();
        for (JsonObject event : events) {
            JsonObject payload = event.getJsonObject("payload", new JsonObject());
            if (!"ENTRY".equals(normalizeUpper(payload.getString("accessType")))) {
                continue;
            }
            int hour = parseTimestamp(extractTimestamp(payload)).getHour();
            byHour.merge(hour, 1, Integer::sum);
        }

        return byHour.entrySet().stream()
                .map(entry -> new PeakHourStat(
                        "peak-" + date + "-" + entry.getKey(),
                        date,
                        entry.getKey(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(PeakHourStat::getHour))
                .toList();
    }

    private List<AreaAttendanceSnapshot> toAreaAttendanceSnapshots(List<JsonObject> events) {
        Map<String, List<JsonObject>> byDate = groupByDate(events);
        List<AreaAttendanceSnapshot> result = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> entry : byDate.entrySet()) {
            result.addAll(toAreaAttendanceByDate(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparing(AreaAttendanceSnapshot::getDate).thenComparing(AreaAttendanceSnapshot::getAreaId));
        return result;
    }

    private List<AreaAttendanceSnapshot> toAreaAttendanceByDate(String date, List<JsonObject> events) {
        Map<String, Integer> entries = new HashMap<>();
        Map<String, Integer> exits = new HashMap<>();

        for (JsonObject event : events) {
            JsonObject payload = event.getJsonObject("payload", new JsonObject());
            String areaId = normalizeString(payload.getString("areaId"));
            if (isBlank(areaId)) {
                continue;
            }
            String direction = normalizeUpper(payload.getString("direction"));
            if ("IN".equals(direction)) {
                entries.merge(areaId, 1, Integer::sum);
            } else if ("OUT".equals(direction)) {
                exits.merge(areaId, 1, Integer::sum);
            }
        }

        Set<String> areas = new HashSet<>();
        areas.addAll(entries.keySet());
        areas.addAll(exits.keySet());

        return areas.stream()
                .sorted()
                .map(areaId -> {
                    int in = entries.getOrDefault(areaId, 0);
                    int out = exits.getOrDefault(areaId, 0);
                    return new AreaAttendanceSnapshot(
                            "area-attendance-" + areaId + "-" + date,
                            date,
                            areaId,
                            Math.max(in - out, 0),
                            in,
                            out
                    );
                })
                .toList();
    }

    private List<AreaPeakHourStat> toAreaPeakHours(List<JsonObject> events) {
        Map<String, List<JsonObject>> byDate = groupByDate(events);
        List<AreaPeakHourStat> result = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> entry : byDate.entrySet()) {
            result.addAll(toAreaPeakHoursByDate(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparing(AreaPeakHourStat::getDate)
                .thenComparing(AreaPeakHourStat::getAreaId)
                .thenComparing(AreaPeakHourStat::getHour));
        return result;
    }

    private List<AreaPeakHourStat> toAreaPeakHoursByDate(String date, List<JsonObject> events) {
        Map<String, Integer> counter = new HashMap<>();
        for (JsonObject event : events) {
            JsonObject payload = event.getJsonObject("payload", new JsonObject());
            if (!"IN".equals(normalizeUpper(payload.getString("direction")))) {
                continue;
            }
            String areaId = normalizeString(payload.getString("areaId"));
            if (isBlank(areaId)) {
                continue;
            }
            int hour = parseTimestamp(extractTimestamp(payload)).getHour();
            counter.merge(areaId + "::" + hour, 1, Integer::sum);
        }

        return counter.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("::", 2);
                    String areaId = parts[0];
                    int hour = Integer.parseInt(parts[1]);
                    return new AreaPeakHourStat(
                            "area-peak-" + areaId + "-" + date + "-" + hour,
                            date,
                            areaId,
                            hour,
                            entry.getValue()
                    );
                })
                .sorted(Comparator.comparing(AreaPeakHourStat::getAreaId).thenComparing(AreaPeakHourStat::getHour))
                .toList();
    }

    private UniqueUsersStat toUniqueUsersStat(String periodType, String periodValue, List<JsonObject> events) {
        Set<String> users = events.stream()
                .map(event -> normalizeString(event.getJsonObject("payload", new JsonObject()).getString("badgeId")))
                .filter(value -> !isBlank(value))
                .collect(Collectors.toSet());

        return new UniqueUsersStat(
                "unique-users-" + periodType + "-" + periodValue,
                periodType,
                periodValue,
                users.size()
        );
    }

    private GymSessionDurationStat toGymSessionDurationStat(String periodType, String periodValue, List<JsonObject> events) {
        List<Double> durations = computeSessionDurations(events, false, null);
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

    private AreaSessionDurationStat toAreaSessionDurationStat(String periodType, String periodValue, String areaId, List<JsonObject> events) {
        List<Double> durations = computeSessionDurations(events, true, areaId);
        double avg = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double max = durations.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        return new AreaSessionDurationStat(
                "area-duration-" + periodType + "-" + areaId + "-" + periodValue,
                periodType,
                periodValue,
                areaId,
                avg,
                max,
                durations.size()
        );
    }

    private List<Double> computeSessionDurations(List<JsonObject> events, boolean areaScoped, String areaIdFilter) {
        List<JsonObject> ordered = sortByTimestamp(events);
        Map<String, LocalDateTime> starts = new HashMap<>();
        List<Double> durations = new ArrayList<>();

        for (JsonObject event : ordered) {
            JsonObject payload = event.getJsonObject("payload", new JsonObject());
            String badgeId = normalizeString(payload.getString("badgeId"));
            if (isBlank(badgeId)) {
                continue;
            }

            String key;
            String startToken;
            String endToken;
            if (areaScoped) {
                String areaId = normalizeString(payload.getString("areaId"));
                if (isBlank(areaId) || !areaId.equals(areaIdFilter)) {
                    continue;
                }
                key = badgeId + "::" + areaId;
                startToken = "IN";
                endToken = "OUT";
            } else {
                key = badgeId;
                startToken = "ENTRY";
                endToken = "EXIT";
            }

            String state = areaScoped
                    ? normalizeUpper(payload.getString("direction"))
                    : normalizeUpper(payload.getString("accessType"));
            LocalDateTime timestamp = parseTimestamp(extractTimestamp(payload));

            if (startToken.equals(state)) {
                starts.put(key, timestamp);
            } else if (endToken.equals(state)) {
                LocalDateTime startedAt = starts.remove(key);
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

    private double computeHourlyUtilizationRate(double totalUsageMinutes) {
        double rawRate = (Math.max(totalUsageMinutes, 0.0) / MINUTES_PER_HOUR) * 100.0;
        return Math.min(rawRate, 100.0);
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

    private String normalizeMonth(String month) {
        if (isBlank(month)) {
            throw new IllegalArgumentException("month cannot be null or empty");
        }
        return YearMonth.parse(month.trim()).toString();
    }

    private String normalizeNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
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