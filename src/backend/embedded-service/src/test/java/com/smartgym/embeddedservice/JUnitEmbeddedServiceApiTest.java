package com.smartgym.embeddedservice;

import com.smartgym.embeddedservice.application.EmbeddedServiceApiImpl;
import com.smartgym.embeddedservice.application.ports.AnalyticsServicePort;
import com.smartgym.embeddedservice.application.ports.AreaServicePort;
import com.smartgym.embeddedservice.application.ports.EmbeddedRepository;
import com.smartgym.embeddedservice.application.ports.MachineServicePort;
import com.smartgym.embeddedservice.model.AreaAccessMessage;
import com.smartgym.embeddedservice.model.GymAccessMessage;
import com.smartgym.embeddedservice.model.MachineUsageMessage;
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
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-01",
                "2026-03-28T09:00:00Z",
                "badge-001",
                "cardio-area",
                "IN"
        );

        service.processAreaAccess(message).join();

        assertEquals(List.of("area-service", "analytics-service", "embedded-repository"), callOrder);
        assertEquals(1, repository.savedEvents.size());
    }

    @Test
    void processAreaExitCallsAreaServiceThenSavesEvent() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

        AreaAccessMessage message = new AreaAccessMessage(
                "reader-01",
                "2026-03-28T09:00:00Z",
                "badge-001",
                "cardio-area",
                "OUT"
        );

        service.processAreaExit(message).join();

        assertEquals(List.of("area-service-exit", "analytics-service", "embedded-repository"), callOrder);
        assertEquals(1, repository.savedEvents.size());
    }

    @Test
    void processAreaAccessFailsValidationAndDoesNotCallDependencies() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

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
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

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

    @Test
    void processGymAccessDoesNotSaveEventWhenAnalyticsFails() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, true);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

        GymAccessMessage message = new GymAccessMessage(
                "turnstile-01",
                "2026-03-28T09:00:00Z",
                "badge-001",
                "ENTRY"
        );

        assertThrows(CompletionException.class, () -> service.processGymAccess(message).join());
        assertEquals(List.of("analytics-service"), callOrder);
        assertEquals(0, repository.savedEvents.size());
    }

    @Test
    void processMachineUsageStartedCallsMachineServiceThenSavesEvent() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

        MachineUsageMessage message = new MachineUsageMessage(
                "machine-reader-01",
                "2026-03-28T10:00:00Z",
                "machine-01",
                "badge-001",
                "STARTED"
        );

        service.processMachineUsage(message).join();

        assertEquals(List.of("machine-service-start", "analytics-service", "embedded-repository"), callOrder);
        assertEquals(1, repository.savedEvents.size());
    }

    @Test
    void processMachineUsageStoppedCallsMachineServiceThenSavesEvent() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

        MachineUsageMessage message = new MachineUsageMessage(
                "machine-reader-01",
                "2026-03-28T10:00:00Z",
                "machine-01",
                null,
                "STOPPED"
        );

        service.processMachineUsage(message).join();

        assertEquals(List.of("machine-service-end", "analytics-service", "embedded-repository"), callOrder);
        assertEquals(1, repository.savedEvents.size());
    }

    @Test
    void processMachineUsageFailsValidationWhenStartedWithoutBadge() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, false);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

        MachineUsageMessage invalidMessage = new MachineUsageMessage(
                "machine-reader-01",
                "2026-03-28T10:00:00Z",
                "machine-01",
                "  ",
                "STARTED"
        );

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> service.processMachineUsage(invalidMessage).join()
        );

        assertNotNull(ex.getCause());
        assertEquals("badgeId is required when usageState is STARTED", ex.getCause().getMessage());
        assertEquals(List.of(), callOrder);
    }

    @Test
    void processMachineUsageDoesNotSaveEventWhenMachineServiceFails() {
        List<String> callOrder = new ArrayList<>();
        RecordingAreaServicePort areaServicePort = new RecordingAreaServicePort(callOrder, false);
        RecordingAnalyticsServicePort analyticsServicePort = new RecordingAnalyticsServicePort(callOrder, false);
        RecordingMachineServicePort machineServicePort = new RecordingMachineServicePort(callOrder, true);
        RecordingEmbeddedRepository repository = new RecordingEmbeddedRepository(callOrder);
        EmbeddedServiceApiImpl service = new EmbeddedServiceApiImpl(
                repository,
                areaServicePort,
                analyticsServicePort,
                machineServicePort
        );

        MachineUsageMessage message = new MachineUsageMessage(
                "machine-reader-01",
                "2026-03-28T10:00:00Z",
                "machine-01",
                "badge-001",
                "STARTED"
        );

        assertThrows(CompletionException.class, () -> service.processMachineUsage(message).join());
        assertEquals(List.of("machine-service-start"), callOrder);
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

        @Override
        public CompletableFuture<Void> processAreaExit(AreaAccessMessage message) {
            callOrder.add("area-service-exit");
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("area-service unavailable"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class RecordingAnalyticsServicePort implements AnalyticsServicePort {

        private final List<String> callOrder;
        private final boolean fail;

        private RecordingAnalyticsServicePort(List<String> callOrder, boolean fail) {
            this.callOrder = callOrder;
            this.fail = fail;
        }

        @Override
        public CompletableFuture<Void> ingestEvent(JsonObject event) {
            callOrder.add("analytics-service");
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("analytics-service unavailable"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class RecordingMachineServicePort implements MachineServicePort {

        private final List<String> callOrder;
        private final boolean fail;

        private RecordingMachineServicePort(List<String> callOrder, boolean fail) {
            this.callOrder = callOrder;
            this.fail = fail;
        }

        @Override
        public CompletableFuture<Void> startSession(MachineUsageMessage message) {
            callOrder.add("machine-service-start");
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("machine-service unavailable"));
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> endSession(MachineUsageMessage message) {
            callOrder.add("machine-service-end");
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("machine-service unavailable"));
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
