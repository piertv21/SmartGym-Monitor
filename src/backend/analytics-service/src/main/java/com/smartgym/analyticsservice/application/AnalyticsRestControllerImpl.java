package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRestController;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import io.vertx.core.json.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/analytics-service")
public class AnalyticsRestControllerImpl implements AnalyticsRestController {

    private final AnalyticsServiceAPI analyticsServiceAPI;

    public AnalyticsRestControllerImpl(AnalyticsServiceAPI analyticsServiceAPI) {
        this.analyticsServiceAPI = analyticsServiceAPI;
    }

    @Override
    @PostMapping("/events/ingest")
    public CompletableFuture<ResponseEntity<?>> ingestEvent(@RequestBody Map<String, Object> event) {
        return analyticsServiceAPI.ingestEvent(new JsonObject(event))
                .thenApply(ignored -> ResponseEntity.accepted().build());
    }

    @Override
    @GetMapping("/attendance/{date}")
    public CompletableFuture<ResponseEntity<?>> getAttendanceStats(@PathVariable String date) {
        return analyticsServiceAPI.getAttendanceStats(date)
                .thenApply(snapshotOpt -> snapshotOpt
                        .<ResponseEntity<?>>map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @Override
    @GetMapping("/attendance")
    public CompletableFuture<ResponseEntity<?>> getAllAttendanceStats() {
        return analyticsServiceAPI.getAllAttendanceStats()
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/machine-utilization")
    public CompletableFuture<ResponseEntity<?>> getMachineUtilization() {
        return analyticsServiceAPI.getMachineUtilization()
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/machine-utilization/{date}")
    public CompletableFuture<ResponseEntity<?>> getMachineUtilizationByDate(@PathVariable String date) {
        return analyticsServiceAPI.getMachineUtilizationByDate(date)
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/peak-hours")
    public CompletableFuture<ResponseEntity<?>> getPeakHours() {
        return analyticsServiceAPI.getPeakHours()
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/peak-hours/{date}")
    public CompletableFuture<ResponseEntity<?>> getPeakHoursByDate(@PathVariable String date) {
        return analyticsServiceAPI.getPeakHoursByDate(date)
                .thenApply(ResponseEntity::ok);
    }
}