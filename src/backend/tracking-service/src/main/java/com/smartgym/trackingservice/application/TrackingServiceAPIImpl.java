package com.smartgym.trackingservice.application;

import com.smartgym.trackingservice.model.EndGymSessionMessage;
import com.smartgym.trackingservice.model.GymSession;
import com.smartgym.trackingservice.model.StartGymSessionMessage;
import com.smartgym.trackingservice.application.ports.TrackingRepository;
import com.smartgym.trackingservice.application.ports.TrackingServiceAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TrackingServiceAPIImpl implements TrackingServiceAPI {

    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceAPIImpl.class);

    private final TrackingRepository repository;

    public TrackingServiceAPIImpl(TrackingRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<GymSession> startGymSession(StartGymSessionMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            validateStartMessage(message);

            repository.findActiveSessionByBadgeId(message.getBadgeId().trim()).join()
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Badge already has an active session: " + existing.getBadgeId());
                    });

            GymSession session = new GymSession(
                    UUID.randomUUID().toString(),
                    message.getBadgeId().trim(),
                    LocalDateTime.now()
            );

            repository.saveGymSession(session).join();
            logger.info("GymSession started: sessionId={}, badgeId={}", session.getGymSessionId(), session.getBadgeId());
            return session;
        });
    }

    @Override
    public CompletableFuture<GymSession> endGymSession(EndGymSessionMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            validateEndMessage(message);

            GymSession activeSession = repository.findActiveSessionByBadgeId(message.getBadgeId().trim()).join()
                    .orElseThrow(() -> new IllegalStateException(
                            "Badge has no active session: " + message.getBadgeId().trim()));

            activeSession.end(LocalDateTime.now());
            repository.saveGymSession(activeSession).join();

            logger.info("GymSession ended: sessionId={}, badgeId={}",
                    activeSession.getGymSessionId(), activeSession.getBadgeId());
            return activeSession;
        });
    }

    @Override
    public CompletableFuture<Long> getGymCount() {
        return repository.countActiveSessions();
    }

    @Override
    public CompletableFuture<List<GymSession>> getActiveSessions() {
        return repository.findActiveSessions();
    }

    private void validateStartMessage(StartGymSessionMessage message) {
        if (Objects.isNull(message)) {
            throw new IllegalArgumentException("StartGymSessionMessage cannot be null");
        }
        if (isBlank(message.getBadgeId())) {
            throw new IllegalArgumentException("badgeId cannot be null or empty");
        }
    }

    private void validateEndMessage(EndGymSessionMessage message) {
        if (Objects.isNull(message)) {
            throw new IllegalArgumentException("EndGymSessionMessage cannot be null");
        }
        if (isBlank(message.getBadgeId())) {
            throw new IllegalArgumentException("badgeId cannot be null or empty");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
