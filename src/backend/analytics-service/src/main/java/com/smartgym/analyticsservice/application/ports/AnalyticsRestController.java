package com.smartgym.analyticsservice.application.ports;

import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;

public interface AnalyticsRestController {

    CompletableFuture<ResponseEntity<?>> getAttendanceStats(String date);

    CompletableFuture<ResponseEntity<?>> getAllAttendanceStats();

    CompletableFuture<ResponseEntity<?>> getMachineUtilization();

    CompletableFuture<ResponseEntity<?>> getMachineUtilizationByDate(String date);

    CompletableFuture<ResponseEntity<?>> getPeakHours();

    CompletableFuture<ResponseEntity<?>> getPeakHoursByDate(String date);
}