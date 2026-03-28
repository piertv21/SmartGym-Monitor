package com.smartgym.analyticsservice.application.ports;

import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsServiceAPI {

    CompletableFuture<Optional<AttendanceSnapshot>> getAttendanceStats(String date);

    CompletableFuture<List<AttendanceSnapshot>> getAllAttendanceStats();

    CompletableFuture<List<MachineUtilization>> getMachineUtilization();

    CompletableFuture<List<MachineUtilization>> getMachineUtilizationByDate(String date);

    CompletableFuture<List<PeakHourStat>> getPeakHours();

    CompletableFuture<List<PeakHourStat>> getPeakHoursByDate(String date);
}