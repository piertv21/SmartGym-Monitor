package com.smartgym.analyticsservice.application.ports;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;

public interface AnalyticsRestController {

    CompletableFuture<ResponseEntity<?>> ingestEvent(Map<String, Object> event);

    CompletableFuture<ResponseEntity<?>> getAllAttendanceStats();

    CompletableFuture<ResponseEntity<?>> getAttendanceSeries(
            String from, String to, String granularity, String areaId);

    CompletableFuture<ResponseEntity<?>> getGymSessionDurationByDate(String date);
}
