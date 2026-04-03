package com.smartgym.embeddedservice.application;

import com.smartgym.embeddedservice.application.ports.EmbeddedRepository;
import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.application.ports.AreaServicePort;
import com.smartgym.embeddedservice.application.ports.AnalyticsServicePort;
import com.smartgym.embeddedservice.application.ports.MachineServicePort;
import com.smartgym.embeddedservice.model.AreaAccessMessage;
import com.smartgym.embeddedservice.model.DeviceStatusMessage;
import com.smartgym.embeddedservice.model.GymAccessMessage;
import com.smartgym.embeddedservice.model.MachineUsageMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.Locale;

public class EmbeddedServiceApiImpl implements EmbeddedServiceAPI {

    private static final String EVENT_ID = "eventId";
    private static final String EVENT_TYPE = "eventType";
    private static final String PAYLOAD = "payload";

    private static final String GYM_ACCESS_EVENT = "GYM_ACCESS";
    private static final String AREA_ACCESS_EVENT = "AREA_ACCESS";
    private static final String MACHINE_USAGE_EVENT = "MACHINE_USAGE";
    private static final String DEVICE_STATUS_EVENT = "DEVICE_STATUS";

    private final EmbeddedRepository embeddedRepository;
    private final AreaServicePort areaServicePort;
    private final AnalyticsServicePort analyticsServicePort;
    private final MachineServicePort machineServicePort;

    public EmbeddedServiceApiImpl(
            EmbeddedRepository embeddedRepository,
            AreaServicePort areaServicePort,
            AnalyticsServicePort analyticsServicePort,
            MachineServicePort machineServicePort
    ) {
        this.embeddedRepository = embeddedRepository;
        this.areaServicePort = areaServicePort;
        this.analyticsServicePort = analyticsServicePort;
        this.machineServicePort = machineServicePort;
    }

    @Override
    public CompletableFuture<Void> processGymAccess(GymAccessMessage message) {
        if (message == null || isBlank(message.getDeviceId()) || isBlank(message.getBadgeId())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid gym access message"));
        }

        JsonObject event = buildGymAccessEvent(message);
        return forwardToAnalyticsAndSave(event);
    }

    @Override
    public CompletableFuture<Void> processAreaAccess(AreaAccessMessage message) {
        if (message == null
                || isBlank(message.getDeviceId())
                || isBlank(message.getBadgeId())
                || isBlank(message.getAreaId())
                || isBlank(message.getDirection())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid area access message"));
        }

        JsonObject event = buildAreaAccessEvent(message);
        return executeWithGuaranteedAnalytics(areaServicePort.processAreaAccess(message), event);
    }

    @Override
    public CompletableFuture<Void> processAreaExit(AreaAccessMessage message) {
        if (message == null
                || isBlank(message.getDeviceId())
                || isBlank(message.getBadgeId())
                || isBlank(message.getAreaId())
                || isBlank(message.getDirection())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid area exit message"));
        }

        JsonObject event = buildAreaAccessEvent(message);
        return executeWithGuaranteedAnalytics(areaServicePort.processAreaExit(message), event);
    }

    @Override
    public CompletableFuture<Void> processMachineUsage(MachineUsageMessage message) {
        if (message == null
                || isBlank(message.getDeviceId())
                || isBlank(message.getMachineId())
                || isBlank(message.getUsageState())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid machine usage message"));
        }

        String usageState = message.getUsageState().trim().toUpperCase(Locale.ROOT);
        CompletableFuture<Void> machineCall;
        if ("STARTED".equals(usageState)) {
            if (isBlank(message.getBadgeId())) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("badgeId is required when usageState is STARTED"));
            }
            machineCall = machineServicePort.startSession(message);
        } else if ("STOPPED".equals(usageState)) {
            machineCall = machineServicePort.endSession(message);
        } else {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unsupported usageState: " + message.getUsageState()));
        }

        JsonObject event = buildMachineUsageEvent(message);
        return executeWithGuaranteedAnalytics(machineCall, event);
    }

    @Override
    public CompletableFuture<Void> processDeviceStatus(DeviceStatusMessage message) {
        if (message == null
                || isBlank(message.getDeviceId())
                || isBlank(message.getDeviceType())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid device status message"));
        }

        JsonObject event = buildDeviceStatusEvent(message);
        return embeddedRepository.saveEvent(event);
    }

    @Override
    public CompletableFuture<JsonArray> getAllEvents() {
        return embeddedRepository.findAllEvents();
    }

    @Override
    public CompletableFuture<JsonArray> getAllEventsByType(String eventType) {
        if (isBlank(eventType)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("eventType cannot be null or empty"));
        }
        return embeddedRepository.findAllEventsByType(eventType);
    }

    @Override
    public CompletableFuture<Optional<JsonObject>> getEventById(String eventId) {
        if (isBlank(eventId)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("eventId cannot be null or empty"));
        }
        return embeddedRepository.findEventById(eventId);
    }

    private JsonObject buildGymAccessEvent(GymAccessMessage message) {
        return new JsonObject()
                .put(EVENT_ID, UUID.randomUUID().toString())
                .put(EVENT_TYPE, GYM_ACCESS_EVENT)
                .put("deviceId", message.getDeviceId())
                .put("timeStamp", message.getTimestamp())
                .put(PAYLOAD, JsonObject.mapFrom(message));
    }

    private JsonObject buildAreaAccessEvent(AreaAccessMessage message) {
        return new JsonObject()
                .put(EVENT_ID, UUID.randomUUID().toString())
                .put(EVENT_TYPE, AREA_ACCESS_EVENT)
                .put("deviceId", message.getDeviceId())
                .put("timeStamp", message.getTimestamp())
                .put(PAYLOAD, JsonObject.mapFrom(message));
    }

    private JsonObject buildMachineUsageEvent(MachineUsageMessage message) {
        return new JsonObject()
                .put(EVENT_ID, UUID.randomUUID().toString())
                .put(EVENT_TYPE, MACHINE_USAGE_EVENT)
                .put("deviceId", message.getDeviceId())
                .put("timeStamp", message.getTimestamp())
                .put(PAYLOAD, JsonObject.mapFrom(message));
    }

    private JsonObject buildDeviceStatusEvent(DeviceStatusMessage message) {
        return new JsonObject()
                .put(EVENT_ID, UUID.randomUUID().toString())
                .put(EVENT_TYPE, DEVICE_STATUS_EVENT)
                .put("deviceId", message.getDeviceId())
                .put("timeStamp", message.getTimestamp())
                .put(PAYLOAD, JsonObject.mapFrom(message));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private CompletableFuture<Void> forwardToAnalyticsAndSave(JsonObject event) {
        return analyticsServicePort.ingestEvent(event)
                .thenCompose(ignored -> embeddedRepository.saveEvent(event));
    }

    private CompletableFuture<Void> executeWithGuaranteedAnalytics(CompletableFuture<Void> domainCall, JsonObject event) {
        return domainCall
                .handle((ignored, domainError) -> domainError)
                .thenCompose(domainError -> forwardToAnalyticsAndSave(event)
                        .handle((ignored, analyticsError) -> {
                            if (domainError != null) {
                                Throwable domainCause = unwrap(domainError);
                                if (analyticsError != null) {
                                    domainCause.addSuppressed(unwrap(analyticsError));
                                }
                                throw new CompletionException(domainCause);
                            }
                            if (analyticsError != null) {
                                throw new CompletionException(unwrap(analyticsError));
                            }
                            return null;
                        }));
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }
}