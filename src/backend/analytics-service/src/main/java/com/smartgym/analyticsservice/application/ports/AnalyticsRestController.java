package com.smartgym.analyticsservice.application.ports;

import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsRestController {

    CompletableFuture<ResponseEntity<?>> ingestEvent(Map<String, Object> event);

    CompletableFuture<ResponseEntity<?>> getAttendanceStats(String date);

    CompletableFuture<ResponseEntity<?>> getAllAttendanceStats();

    CompletableFuture<ResponseEntity<?>> getAttendanceSeries(String from, String to, String granularity, String areaId);


    CompletableFuture<ResponseEntity<?>> getMachineUtilization();

    CompletableFuture<ResponseEntity<?>> getMachineUtilizationByDate(String date);

    CompletableFuture<ResponseEntity<?>> getMachineUtilizationByMonth(String month);

    CompletableFuture<ResponseEntity<?>> getUniqueUsersByDate(String date);

    CompletableFuture<ResponseEntity<?>> getUniqueUsersByMonth(String month);

    CompletableFuture<ResponseEntity<?>> getGymSessionDurationByDate(String date);

    CompletableFuture<ResponseEntity<?>> getGymSessionDurationByMonth(String month);

    CompletableFuture<ResponseEntity<?>> getAreaSessionDurationByDate(String date, String areaId);

    CompletableFuture<ResponseEntity<?>> getAreaSessionDurationByMonth(String month, String areaId);

    CompletableFuture<ResponseEntity<?>> getPeakHours();

    CompletableFuture<ResponseEntity<?>> getPeakHoursByDate(String date);

    CompletableFuture<ResponseEntity<?>> getAreaAttendance();

    CompletableFuture<ResponseEntity<?>> getAreaAttendanceByDate(String date);

    CompletableFuture<ResponseEntity<?>> getAreaAttendanceByDateAndAreaId(String date, String areaId);

    CompletableFuture<ResponseEntity<?>> getAreaPeakHours();

    CompletableFuture<ResponseEntity<?>> getAreaPeakHoursByDate(String date);

    CompletableFuture<ResponseEntity<?>> getAreaPeakHoursByDateAndAreaId(String date, String areaId);
}