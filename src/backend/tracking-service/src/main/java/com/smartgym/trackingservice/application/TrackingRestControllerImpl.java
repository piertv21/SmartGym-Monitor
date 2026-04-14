package com.smartgym.trackingservice.application;

import com.smartgym.trackingservice.application.ports.TrackingRestController;
import com.smartgym.trackingservice.application.ports.TrackingServiceAPI;
import com.smartgym.trackingservice.model.EndGymSessionMessage;
import com.smartgym.trackingservice.model.StartGymSessionMessage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrackingRestControllerImpl implements TrackingRestController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingRestControllerImpl.class);

    private final TrackingServiceAPI trackingService;

    public TrackingRestControllerImpl(TrackingServiceAPI trackingService) {
        this.trackingService = trackingService;
        logger.info("✅ TrackingRestControllerImpl initialized");
    }

    @Override
    @PostMapping("/start-session")
    public CompletableFuture<ResponseEntity<?>> startGymSession(
            @RequestBody StartGymSessionMessage message) {
        return trackingService
                .startGymSession(message)
                .thenApply(
                        session ->
                                ResponseEntity.ok(
                                        Map.of(
                                                "message", "Gym session started",
                                                "gymSessionId", session.getGymSessionId(),
                                                "badgeId", session.getBadgeId(),
                                                "startTime", session.getStartTime().toString())));
    }

    @Override
    @PostMapping("/end-session")
    public CompletableFuture<ResponseEntity<?>> endGymSession(
            @RequestBody EndGymSessionMessage message) {
        return trackingService
                .endGymSession(message)
                .thenApply(
                        session ->
                                ResponseEntity.ok(
                                        Map.of(
                                                "message", "Gym session ended",
                                                "gymSessionId", session.getGymSessionId(),
                                                "badgeId", session.getBadgeId(),
                                                "endTime", String.valueOf(session.getEndTime()))));
    }

    @Override
    @GetMapping("/count")
    public CompletableFuture<ResponseEntity<?>> getGymCount() {
        return trackingService
                .getGymCount()
                .thenApply(count -> ResponseEntity.ok(Map.of("gymCount", count)));
    }

    @Override
    @GetMapping("/active-sessions")
    public CompletableFuture<ResponseEntity<?>> getActiveSessions() {
        return trackingService.getActiveSessions().thenApply(ResponseEntity::ok);
    }
}
