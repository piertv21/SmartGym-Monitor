package com.smartgym.analyticsservice.application.ports;

import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaAttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaPeakHourStat;
import com.smartgym.analyticsservice.model.AreaSessionDurationStat;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.GymSessionDurationStat;
import com.smartgym.analyticsservice.model.PeakHourStat;
import com.smartgym.analyticsservice.model.UniqueUsersStat;
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

    CompletableFuture<List<MachineUtilization>> getMachineUtilizationByMonth(String month);

    CompletableFuture<UniqueUsersStat> getUniqueUsersByDate(String date);

    CompletableFuture<UniqueUsersStat> getUniqueUsersByMonth(String month);

    CompletableFuture<GymSessionDurationStat> getGymSessionDurationByDate(String date);

    CompletableFuture<GymSessionDurationStat> getGymSessionDurationByMonth(String month);

    CompletableFuture<AreaSessionDurationStat> getAreaSessionDurationByDate(String date, String areaId);

    CompletableFuture<AreaSessionDurationStat> getAreaSessionDurationByMonth(String month, String areaId);

    CompletableFuture<List<PeakHourStat>> getPeakHours();

    CompletableFuture<List<PeakHourStat>> getPeakHoursByDate(String date);

    CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendance();

    CompletableFuture<List<AreaAttendanceSnapshot>> getAreaAttendanceByDate(String date);

    CompletableFuture<Optional<AreaAttendanceSnapshot>> getAreaAttendanceByDateAndAreaId(String date, String areaId);

    CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHours();

    CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDate(String date);

    CompletableFuture<List<AreaPeakHourStat>> getAreaPeakHoursByDateAndAreaId(String date, String areaId);
}