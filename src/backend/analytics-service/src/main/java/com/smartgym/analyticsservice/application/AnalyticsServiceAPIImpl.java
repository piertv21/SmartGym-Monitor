package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AnalyticsServiceAPIImpl implements AnalyticsServiceAPI {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsServiceAPIImpl(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
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
}