package com.smartgym.embeddedservice;

import com.smartgym.embeddedservice.application.EmbeddedServiceApiImpl;
import com.smartgym.embeddedservice.application.ports.AreaServicePort;
import com.smartgym.embeddedservice.application.ports.EmbeddedRepository;
import com.smartgym.embeddedservice.model.AreaAccessMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JUnitEmbeddedServiceApiTest {

    @Test
    void processAreaAccessCallsAreaServiceThenSavesEvent() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(repository, areaServicePort);

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-01",
                "2026-03-28T09:00:00Z",
                "badge-001",
                "cardio-area",
                "IN"
        );

        service.processAreaAccess(message).join();

        assertEquals(List.of("area-service", "embedded-repository"), callOrder);
        assertEquals(1, repository.savedEvents.size());
    }

    @Test
    void processAreaAccessFailsValidationAndDoesNotCallDependencies() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(repository, areaServicePort);

        AreaAccessMessage invalidMessage = new AreaAccessMessage(
                "reader-01",
                "2026-03-28T09:00:00Z",
                "badge-001",
                " ",
                "IN"
        );

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> service.processAreaAccess(invalidMessage).join()
        );

        assertNotNull(ex.getCause());
        assertEquals("Invalid area access message", ex.getCause().getMessage());
        assertEquals(List.of(), callOrder);
    }

    @Test
    void processAreaAccessDoesNotSaveEventWhenAreaServiceFails() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, true);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(repository, areaServicePort);

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-01",
                "2026-03-28T09:00:00Z",
                "badge-001",
                "cardio-area",
                "IN"
        );

        assertThrows(CompletionException.class, () -> service.processAreaAccess(message).join());
        assertEquals(List.of("area-service"), callOrder);
        assertEquals(0, repository.savedEvents.size());
    }

    private static final class RecordingAreaServicePort implements AreaServicePort {

        private final List<String> callOrder;
        private final boolean fail;

        private RecordingAreaServicePort(List<String> callOrder, boolean fail) {
            this.callOrder = callOrder;
            this.fail = fail;
        }

        @Override
        public CompletableFuture<Void> processAreaAccess(AreaAccessMessage message) {
            callOrder.add("area-service");
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("area-service unavailable"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class RecordingEmbeddedRepository implements EmbeddedRepository {

        private final List<String> callOrder;
        private final List<JsonObject> savedEvents = new ArrayList<>();

        private RecordingEmbeddedRepository(List<String> callOrder) {
            this.callOrder = callOrder;
        }

        @Override
        public CompletableFuture<Void> saveEvent(JsonObject event) {
            callOrder.add("embedded-repository");
            savedEvents.add(event);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<JsonObject>> findEventById(String eventId) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<JsonArray> findAllEvents() {
            return CompletableFuture.completedFuture(new JsonArray());
        }

        @Override
        public CompletableFuture<JsonArray> findAllEventsByType(String eventType) {
            return CompletableFuture.completedFuture(new JsonArray());
        }
    }
}

