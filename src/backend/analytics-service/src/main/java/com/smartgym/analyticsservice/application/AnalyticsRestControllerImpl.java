package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRestController;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import io.vertx.core.json.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
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

    @Override
    @GetMapping("/area-attendance")
    public CompletableFuture<ResponseEntity<?>> getAreaAttendance() {
        return analyticsServiceAPI.getAreaAttendance()
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/area-attendance/{date}")
    public CompletableFuture<ResponseEntity<?>> getAreaAttendanceByDate(@PathVariable String date) {
        return analyticsServiceAPI.getAreaAttendanceByDate(date)
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/area-attendance/{date}/{areaId}")
    public CompletableFuture<ResponseEntity<?>> getAreaAttendanceByDateAndAreaId(
            @PathVariable String date,
            @PathVariable String areaId
    ) {
        return analyticsServiceAPI.getAreaAttendanceByDateAndAreaId(date, areaId)
                .thenApply(snapshotOpt -> snapshotOpt
                        .<ResponseEntity<?>>map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @Override
    @GetMapping("/area-peak-hours")
    public CompletableFuture<ResponseEntity<?>> getAreaPeakHours() {
        return analyticsServiceAPI.getAreaPeakHours()
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/area-peak-hours/{date}")
    public CompletableFuture<ResponseEntity<?>> getAreaPeakHoursByDate(@PathVariable String date) {
        return analyticsServiceAPI.getAreaPeakHoursByDate(date)
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/area-peak-hours/{date}/{areaId}")
    public CompletableFuture<ResponseEntity<?>> getAreaPeakHoursByDateAndAreaId(
            @PathVariable String date,
            @PathVariable String areaId
    ) {
        return analyticsServiceAPI.getAreaPeakHoursByDateAndAreaId(date, areaId)
                .thenApply(ResponseEntity::ok);
    }
}