package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AnalyticsServiceAPIImpl implements AnalyticsServiceAPI {

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

        return switch (eventType) {
            case "GYM_ACCESS" -> processGymAccess(payload);
            case "MACHINE_USAGE" -> processMachineUsage(payload);
            case "AREA_ACCESS" -> CompletableFuture.completedFuture(null);
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

        final String date;
        try {
            date = extractDate(timestamp);
        } catch (IllegalArgumentException ex) {
            return CompletableFuture.failedFuture(ex);
        }

        return analyticsRepository.findMachineUtilizationsByDate(date)
                .thenCompose(utilizations -> {
                    MachineUtilization utilization = utilizations.stream()
                            .filter(item -> machineId.equals(item.getMachineId()))
                            .findFirst()
                            .orElseGet(() -> new MachineUtilization(
                                    "machine-" + machineId + "-" + date,
                                    machineId,
                                    date,
                                    0,
                                    0.0,
                                    0.0
                            ));

                    if ("STARTED".equals(usageState)) {
                        int usageCount = Optional.ofNullable(utilization.getUsageCount()).orElse(0);
                        utilization.setUsageCount(usageCount + 1);
                    }

                    return analyticsRepository.saveMachineUtilization(utilization);
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