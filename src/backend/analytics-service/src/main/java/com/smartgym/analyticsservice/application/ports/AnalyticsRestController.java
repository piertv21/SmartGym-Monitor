package com.smartgym.analyticsservice.application.ports;

import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsRestController {

    CompletableFuture<ResponseEntity<?>> ingestEvent(Map<String, Object> event);

    CompletableFuture<ResponseEntity<?>> getAttendanceStats(String date);

    CompletableFuture<ResponseEntity<?>> getAllAttendanceStats();

    CompletableFuture<ResponseEntity<?>> getMachineUtilization();

    CompletableFuture<ResponseEntity<?>> getMachineUtilizationByDate(String date);

    CompletableFuture<ResponseEntity<?>> getPeakHours();

    CompletableFuture<ResponseEntity<?>> getPeakHoursByDate(String date);

    CompletableFuture<ResponseEntity<?>> getAreaAttendance();

    CompletableFuture<ResponseEntity<?>> getAreaAttendanceByDate(String date);

    CompletableFuture<ResponseEntity<?>> getAreaAttendanceByDateAndAreaId(String date, String areaId);

    CompletableFuture<ResponseEntity<?>> getAreaPeakHours();

    CompletableFuture<ResponseEntity<?>> getAreaPeakHoursByDate(String date);

    CompletableFuture<ResponseEntity<?>> getAreaPeakHoursByDateAndAreaId(String date, String areaId);
}