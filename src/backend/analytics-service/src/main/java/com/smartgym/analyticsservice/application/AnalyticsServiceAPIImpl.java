package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaAttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaPeakHourStat;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AnalyticsServiceAPIImpl implements AnalyticsServiceAPI {

    private static final double MINUTES_PER_HOUR = 60.0;

    private final AnalyticsRepository analyticsRepository;
    private final Map<String, LocalDateTime> machineSessionStarts = new ConcurrentHashMap<>();

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

        return switch (eventType) {
            case "GYM_ACCESS" -> processGymAccess(payload);
            case "MACHINE_USAGE" -> processMachineUsage(payload);
            case "AREA_ACCESS" -> processAreaAccess(payload);
            default -> CompletableFuture.completedFuture(null);
        };
    }

    @Override
    public CompletableFuture<Optional<AttendanceSnapshot>> getAttendanceStats(String date) {
        if (isBlank(date)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("date cannot be null or empty")
            );
        }

        return analyticsRepository.findAttendanceByDate(date);
    }

    @Override
    public CompletableFuture<List<AttendanceSnapshot>> getAllAttendanceStats() {
        return analyticsRepository.findAllAttendanceSnapshots();
    }

    @Override
    public CompletableFuture<List<MachineUtilization>> getMachineUtilization() {
        return analyticsRepository.findAllMachineUtilizations();
    }

    @Override
    public CompletableFuture<List<MachineUtilization>> getMachineUtilizationByDate(String date) {
        if (isBlank(date)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("date cannot be null or empty")
            );
        }

        return analyticsRepository.findMachineUtilizationsByDate(date);
    }

    @Override
    public CompletableFuture<List<PeakHourStat>> getPeakHours() {
        return analyticsRepository.findAllPeakHours();
    }

    @Override
    public CompletableFuture<List<PeakHourStat>> getPeakHoursByDate(String date) {
        if (isBlank(date)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("date cannot be null or empty")
            );
        }

        return analyticsRepository.findPeakHoursByDate(date);
    }

    @Override
    public CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendance() {
        return analyticsRepository.findAllAreaAttendanceSnapshots();
    }

    @Override
    public CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendanceByDate(String date) {
        if (isBlank(date)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("date cannot be null or empty")
            );
        }
        return analyticsRepository.findAreaAttendanceByDate(date);
    }

    @Override
    public CompletableFuture<Optional<AreaAttendanceSnapshot>> getAreaAttendanceByDateAndAreaId(String date, String areaId) {
        if (isBlank(date)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("date cannot be null or empty")
            );
        }
        if (isBlank(areaId)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("areaId cannot be null or empty")
            );
        }
        return analyticsRepository.findAreaAttendanceByDateAndAreaId(date, areaId.trim());
    }

    @Override
    public CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHours() {
        return analyticsRepository.findAllAreaPeakHours();
    }

    @Override
    public CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDate(String date) {
        if (isBlank(date)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("date cannot be null or empty")
            );
        }
        return analyticsRepository.findAreaPeakHoursByDate(date);
    }

    @Override
    public CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDateAndAreaId(String date, String areaId) {
        if (isBlank(date)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("date cannot be null or empty")
            );
        }
        if (isBlank(areaId)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("areaId cannot be null or empty")
            );
        }
        return analyticsRepository.findAreaPeakHoursByDateAndAreaId(date, areaId.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private CompletableFuture<Void> processGymAccess(JsonObject payload) {
        String accessType = payload.getString("accessType", "").trim().toUpperCase();
        String timestamp = extractTimestamp(payload);

        if (isBlank(timestamp)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Gym access payload requires timeStamp or timestamp"));
        }

        if (!"ENTRY".equals(accessType) && !"EXIT".equals(accessType)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Gym access payload requires accessType ENTRY or EXIT"));
        }

        final String date;
        final int hour;
        try {
            date = extractDate(timestamp);
            hour = extractHour(timestamp);
        } catch (IllegalArgumentException ex) {
            return CompletableFuture.failedFuture(ex);
        }

        return analyticsRepository.findAttendanceByDate(date)
                .thenCompose(existingOpt -> {
                    AttendanceSnapshot snapshot = existingOpt.orElseGet(() -> new AttendanceSnapshot(
                            "attendance-" + date,
                            date,
                            0,
                            0,
                            0
                    ));

                    int entries = Optional.ofNullable(snapshot.getTotalEntries()).orElse(0);
                    int exits = Optional.ofNullable(snapshot.getTotalExits()).orElse(0);

                    if ("ENTRY".equals(accessType)) {
                        entries += 1;
                    } else {
                        exits += 1;
                    }

                    snapshot.setTotalEntries(entries);
                    snapshot.setTotalExits(exits);
                    snapshot.setGymCount(Math.max(entries - exits, 0));

                    CompletableFuture<Void> saveAttendance = analyticsRepository.saveAttendanceSnapshot(snapshot);
                    if (!"ENTRY".equals(accessType)) {
                        return saveAttendance;
                    }

                    CompletableFuture<Void> savePeak = analyticsRepository.findPeakHoursByDate(date)
                            .thenCompose(stats -> {
                                PeakHourStat peakHourStat = stats.stream()
                                        .filter(stat -> stat.getHour() != null && stat.getHour() == hour)
                                        .findFirst()
                                        .orElseGet(() -> new PeakHourStat(
                                                "peak-" + date + "-" + hour,
                                                date,
                                                hour,
                                                0
                                        ));

                                int current = Optional.ofNullable(peakHourStat.getAttendanceCount()).orElse(0);
                                peakHourStat.setAttendanceCount(current + 1);
                                return analyticsRepository.savePeakHourStat(peakHourStat);
                            });

                    return saveAttendance.thenCompose(ignored -> savePeak);
                });
    }

    private CompletableFuture<Void> processMachineUsage(JsonObject payload) {
        String machineId = payload.getString("machineId");
        String usageState = payload.getString("usageState", "").trim().toUpperCase();
        String timestamp = extractTimestamp(payload);

        if (isBlank(machineId) || isBlank(timestamp)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Machine usage payload requires machineId and timeStamp/timestamp"));
        }

        final LocalDateTime eventDateTime;
        final String date;
        try {
            eventDateTime = parseTimestamp(timestamp);
            date = eventDateTime.toLocalDate().toString();
        } catch (IllegalArgumentException ex) {
            return CompletableFuture.failedFuture(ex);
        }

        final String normalizedMachineId = machineId.trim();
        final String sessionKey = buildMachineSessionKey(normalizedMachineId, date);

        return analyticsRepository.findMachineUtilizationsByDate(date)
                .thenCompose(utilizations -> {
                    MachineUtilization utilization = utilizations.stream()
                            .filter(item -> normalizedMachineId.equals(item.getMachineId()))
                            .findFirst()
                            .orElseGet(() -> new MachineUtilization(
                                    "machine-" + normalizedMachineId + "-" + date,
                                    normalizedMachineId,
                                    date,
                                    0,
                                    0.0,
                                    0.0
                            ));

                    int usageCount = Optional.ofNullable(utilization.getUsageCount()).orElse(0);
                    double totalUsageMinutes = Optional.ofNullable(utilization.getTotalUsageMinutes()).orElse(0.0);

                    if ("STARTED".equals(usageState)) {
                        utilization.setUsageCount(usageCount + 1);
                        machineSessionStarts.put(sessionKey, eventDateTime);
                    } else if ("STOPPED".equals(usageState)) {
                        LocalDateTime startedAt = machineSessionStarts.remove(sessionKey);
                        if (startedAt != null && !eventDateTime.isBefore(startedAt)) {
                            double deltaMinutes = Duration.between(startedAt, eventDateTime).toMillis() / 60000.0;
                            totalUsageMinutes += deltaMinutes;
                        }
                    }

                    utilization.setTotalUsageMinutes(totalUsageMinutes);
                    utilization.setUtilizationRate(computeHourlyUtilizationRate(totalUsageMinutes));

                    return analyticsRepository.saveMachineUtilization(utilization);
                });
    }

    private String buildMachineSessionKey(String machineId, String date) {
        return machineId + "::" + date;
    }

    private double computeHourlyUtilizationRate(double totalUsageMinutes) {
        double normalizedMinutes = Math.max(totalUsageMinutes, 0.0);
        double rawRate = (normalizedMinutes / MINUTES_PER_HOUR) * 100.0;
        return Math.min(rawRate, 100.0);
    }

    private CompletableFuture<Void> processAreaAccess(JsonObject payload) {
        String areaId = payload.getString("areaId");
        String direction = payload.getString("direction", "").trim().toUpperCase();
        String timestamp = extractTimestamp(payload);

        if (isBlank(areaId) || isBlank(timestamp)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Area access payload requires areaId and timeStamp/timestamp"));
        }

        if (!"IN".equals(direction) && !"OUT".equals(direction)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Area access payload requires direction IN or OUT"));
        }

        final String normalizedAreaId = areaId.trim();
        final String date;
        final int hour;
        try {
            date = extractDate(timestamp);
            hour = extractHour(timestamp);
        } catch (IllegalArgumentException ex) {
            return CompletableFuture.failedFuture(ex);
        }

        return analyticsRepository.findAreaAttendanceByDateAndAreaId(date, normalizedAreaId)
                .thenCompose(existingOpt -> {
                    AreaAttendanceSnapshot snapshot = existingOpt.orElseGet(() -> new AreaAttendanceSnapshot(
                            "area-attendance-" + normalizedAreaId + "-" + date,
                            date,
                            normalizedAreaId,
                            0,
                            0,
                            0
                    ));

                    int current = Optional.ofNullable(snapshot.getCurrentCount()).orElse(0);
                    int entries = Optional.ofNullable(snapshot.getTotalEntries()).orElse(0);
                    int exits = Optional.ofNullable(snapshot.getTotalExits()).orElse(0);

                    if ("IN".equals(direction)) {
                        current += 1;
                        entries += 1;
                    } else {
                        current = Math.max(current - 1, 0);
                        exits += 1;
                    }

                    snapshot.setCurrentCount(current);
                    snapshot.setTotalEntries(entries);
                    snapshot.setTotalExits(exits);
                    final int currentCount = current;

                    CompletableFuture<Void> saveAreaAttendance = analyticsRepository.saveAreaAttendanceSnapshot(snapshot);
                    if (!"IN".equals(direction)) {
                        return saveAreaAttendance;
                    }

                    CompletableFuture<Void> saveAreaPeak = analyticsRepository
                            .findAreaPeakHoursByDateAndAreaId(date, normalizedAreaId)
                            .thenCompose(stats -> {
                                AreaPeakHourStat areaPeakHourStat = stats.stream()
                                        .filter(stat -> stat.getHour() != null && stat.getHour() == hour)
                                        .findFirst()
                                        .orElseGet(() -> new AreaPeakHourStat(
                                                "area-peak-" + normalizedAreaId + "-" + date + "-" + hour,
                                                date,
                                                normalizedAreaId,
                                                hour,
                                                0
                                        ));

                                int currentPeak = Optional.ofNullable(areaPeakHourStat.getAttendanceCount()).orElse(0);
                                areaPeakHourStat.setAttendanceCount(Math.max(currentPeak, currentCount));
                                return analyticsRepository.saveAreaPeakHourStat(areaPeakHourStat);
                            });

                    return saveAreaAttendance.thenCompose(ignored -> saveAreaPeak);
                });
    }

    private String extractDate(String timestamp) {
        return parseTimestamp(timestamp).toLocalDate().toString();
    }

    private int extractHour(String timestamp) {
        return parseTimestamp(timestamp).getHour();
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(timestamp), ZoneOffset.UTC);
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
        return payload.getString("timestamp");
    }
}