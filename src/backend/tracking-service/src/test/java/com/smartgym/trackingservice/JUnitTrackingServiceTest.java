package com.smartgym.trackingservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.trackingservice.application.TrackingServiceAPIImpl;
import com.smartgym.trackingservice.application.ports.TrackingRepository;
import com.smartgym.trackingservice.model.EndGymSessionMessage;
import com.smartgym.trackingservice.model.GymSession;
import com.smartgym.trackingservice.model.StartGymSessionMessage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JUnitTrackingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startGymSessionCreatesActiveSession() {
        TrackingRepository repository = new InMemoryTrackingRepository();
        TrackingServiceAPIImpl service = new TrackingServiceAPIImpl(repository);

        GymSession session = service.startGymSession(new StartGymSessionMessage("badge-01")).join();

        assertNotNull(session.getGymSessionId());
        assertEquals("badge-01", session.getBadgeId());
        assertTrue(session.isActive());
    }

    @Test
    void startGymSessionFailsWhenBadgeAlreadyActive() {
        InMemoryTrackingRepository repository = new InMemoryTrackingRepository();
        repository.saveGymSession(new GymSession("s-1", "badge-01", LocalDateTime.now())).join();
        TrackingServiceAPIImpl service = new TrackingServiceAPIImpl(repository);

        CompletionException ex =
                assertThrows(
                        CompletionException.class,
                        () ->
                                service.startGymSession(new StartGymSessionMessage("badge-01"))
                                        .join());

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    void endGymSessionClosesExistingSession() {
        InMemoryTrackingRepository repository = new InMemoryTrackingRepository();
        TrackingServiceAPIImpl service = new TrackingServiceAPIImpl(repository);

        GymSession started = service.startGymSession(new StartGymSessionMessage("badge-02")).join();
        GymSession ended = service.endGymSession(new EndGymSessionMessage("badge-02")).join();

        assertEquals(started.getGymSessionId(), ended.getGymSessionId());
        assertNotNull(ended.getEndTime());
        assertTrue(!ended.isActive());
    }

    @Test
    void gymCountReflectsActiveSessions() {
        TrackingRepository repository = new InMemoryTrackingRepository();
        TrackingServiceAPIImpl service = new TrackingServiceAPIImpl(repository);

        service.startGymSession(new StartGymSessionMessage("badge-10")).join();
        service.startGymSession(new StartGymSessionMessage("badge-11")).join();
        service.endGymSession(new EndGymSessionMessage("badge-10")).join();

        long count = service.getGymCount().join();
        assertEquals(1L, count);
    }

    @Test
    void startGymSessionMessageSupportsJsonSerializationAndEqualityContracts() throws Exception {
        StartGymSessionMessage message = new StartGymSessionMessage("badge-20");

        String json = objectMapper.writeValueAsString(message);
        StartGymSessionMessage decoded = objectMapper.readValue(json, StartGymSessionMessage.class);

        assertEquals("{\"badgeId\":\"badge-20\"}", json);
        assertEquals(message, decoded);
        assertEquals(message.hashCode(), decoded.hashCode());
        assertTrue(message.toString().contains("badge-20"));
    }

    @Test
    void endGymSessionMessageSupportsJsonSerializationAndEqualityContracts() throws Exception {
        EndGymSessionMessage message = new EndGymSessionMessage("badge-21");

        String json = objectMapper.writeValueAsString(message);
        EndGymSessionMessage decoded = objectMapper.readValue(json, EndGymSessionMessage.class);

        assertEquals("{\"badgeId\":\"badge-21\"}", json);
        assertEquals(message, decoded);
        assertEquals(message.hashCode(), decoded.hashCode());
        assertTrue(message.toString().contains("badge-21"));
    }

    private static final class InMemoryTrackingRepository implements TrackingRepository {

        private final Map<String, GymSession> sessionsById = new LinkedHashMap<>();

        @Override
        public CompletableFuture<Void> saveGymSession(GymSession session) {
            sessionsById.put(session.getGymSessionId(), session);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<GymSession>> findActiveSessionByBadgeId(String badgeId) {
            return CompletableFuture.completedFuture(
                    sessionsById.values().stream()
                            .filter(s -> s.getBadgeId().equals(badgeId))
                            .filter(GymSession::isActive)
                            .findFirst());
        }

        @Override
        public CompletableFuture<List<GymSession>> findActiveSessions() {
            List<GymSession> sessions =
                    sessionsById.values().stream()
                            .filter(GymSession::isActive)
                            .sorted(Comparator.comparing(GymSession::getStartTime).reversed())
                            .collect(Collectors.toCollection(ArrayList::new));
            return CompletableFuture.completedFuture(sessions);
        }

        @Override
        public CompletableFuture<Long> countActiveSessions() {
            long count = sessionsById.values().stream().filter(GymSession::isActive).count();
            return CompletableFuture.completedFuture(count);
        }
    }
}
