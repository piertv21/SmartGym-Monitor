package com.smartgym.analyticsservice.application.ports;

import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaAttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaPeakHourStat;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsRepository {

    CompletableFuture<Void> saveAttendanceSnapshot(AttendanceSnapshot snapshot);

    CompletableFuture<Optional<AttendanceSnapshot>> findAttendanceByDate(String date);

    CompletableFuture<List<AttendanceSnapshot>> findAllAttendanceSnapshots();

    CompletableFuture<Void> saveMachineUtilization(MachineUtilization machineUtilization);

    CompletableFuture<List<MachineUtilization>> findAllMachineUtilizations();

    CompletableFuture<List<MachineUtilization>> findMachineUtilizationsByDate(String date);

    CompletableFuture<Void> savePeakHourStat(PeakHourStat peakHourStat);

    CompletableFuture<List<PeakHourStat>> findPeakHoursByDate(String date);

    CompletableFuture<List<PeakHourStat>> findAllPeakHours();

    CompletableFuture<Void> saveAreaAttendanceSnapshot(AreaAttendanceSnapshot snapshot);

    CompletableFuture<Optional<AreaAttendanceSnapshot>> findAreaAttendanceByDateAndAreaId(String date, String areaId);

    CompletableFuture<List<AreaAttendanceSnapshot>> findAreaAttendanceByDate(String date);

    CompletableFuture<List<AreaAttendanceSnapshot>> findAllAreaAttendanceSnapshots();

    CompletableFuture<Void> saveAreaPeakHourStat(AreaPeakHourStat areaPeakHourStat);

    CompletableFuture<List<AreaPeakHourStat>> findAreaPeakHoursByDate(String date);

    CompletableFuture<List<AreaPeakHourStat>> findAreaPeakHoursByDateAndAreaId(String date, String areaId);

    CompletableFuture<List<AreaPeakHourStat>> findAllAreaPeakHours();
}