package com.smartgym.trackingservice.application.ports;

import com.smartgym.trackingservice.ddd.Repository;
import com.smartgym.trackingservice.model.GymSession;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TrackingRepository extends Repository {

    CompletableFuture<Void> saveGymSession(GymSession session);

    CompletableFuture<Optional<GymSession>> findActiveSessionByBadgeId(String badgeId);

    CompletableFuture<List<GymSession>> findActiveSessions();

    CompletableFuture<Long> countActiveSessions();
}
