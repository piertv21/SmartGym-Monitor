package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRestController;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AnalyticsRestControllerImpl implements AnalyticsRestController {

    private final AnalyticsServiceAPI analyticsServiceAPI;

    public AnalyticsRestControllerImpl(AnalyticsServiceAPI analyticsServiceAPI) {
        this.analyticsServiceAPI = analyticsServiceAPI;
    }

    @Override
    @PostMapping("/events/ingest")
    public CompletableFuture<ResponseEntity<?>> ingestEvent(
            @RequestBody Map<String, Object> event) {
        return analyticsServiceAPI
                .ingestEvent(new JsonObject(event))
                .thenApply(ignored -> ResponseEntity.accepted().build());
    }

    @Override
    @GetMapping("/attendance")
    public CompletableFuture<ResponseEntity<?>> getAllAttendanceStats() {
        return analyticsServiceAPI.getAllAttendanceStats().thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/attendance/series")
    public CompletableFuture<ResponseEntity<?>> getAttendanceSeries(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "daily") String granularity,
            @RequestParam(required = false) String areaId) {
        return analyticsServiceAPI
                .getAttendanceSeries(from, to, granularity, areaId)
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/gym-session-duration/{date}")
    public CompletableFuture<ResponseEntity<?>> getGymSessionDurationByDate(
            @PathVariable String date) {
        return analyticsServiceAPI.getGymSessionDurationByDate(date).thenApply(ResponseEntity::ok);
    }
}
