package com.smartgym.analyticsservice.application.ports;

import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaAttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaPeakHourStat;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsServiceAPI {

    CompletableFuture<Void> ingestEvent(JsonObject event);

    CompletableFuture<Optional<AttendanceSnapshot>> getAttendanceStats(String date);

    CompletableFuture<List<AttendanceSnapshot>> getAllAttendanceStats();

    CompletableFuture<List<MachineUtilization>> getMachineUtilization();

    CompletableFuture<List<MachineUtilization>> getMachineUtilizationByDate(String date);

    CompletableFuture<List<PeakHourStat>> getPeakHours();

    CompletableFuture<List<PeakHourStat>> getPeakHoursByDate(String date);

    CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendance();

    CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendanceByDate(String date);

    CompletableFuture<Optional<AreaAttendanceSnapshot>> getAreaAttendanceByDateAndAreaId(String date, String areaId);

    CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHours();

    CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDate(String date);

    CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDateAndAreaId(String date, String areaId);
}