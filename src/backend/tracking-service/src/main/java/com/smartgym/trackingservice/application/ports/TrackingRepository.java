package com.smartgym.trackingservice.application.ports;

import com.smartgym.trackingservice.model.GymSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port per l'accesso ai dati
 *
 */
public interface TrackingRepository {

	CompletableFuture<Void> saveGymSession(GymSession session);

	CompletableFuture<Optional<GymSession>> findActiveSessionByBadgeId(String badgeId);

	CompletableFuture<List<GymSession>> findActiveSessions();

	CompletableFuture<Long> countActiveSessions();
}