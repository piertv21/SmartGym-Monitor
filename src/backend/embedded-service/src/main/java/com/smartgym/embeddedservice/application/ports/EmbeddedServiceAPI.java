package com.smartgym.embeddedservice.application.ports;

import com.smartgym.embeddedservice.model.AreaAccessMessage;
import com.smartgym.embeddedservice.model.DeviceStatusMessage;
import com.smartgym.embeddedservice.model.GymAccessMessage;
import com.smartgym.embeddedservice.model.MachineUsageMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EmbeddedServiceAPI {

    CompletableFuture<Void> processGymAccess(GymAccessMessage message);

    CompletableFuture<Void> processAreaAccess(AreaAccessMessage message);

    CompletableFuture<Void> processAreaExit(AreaAccessMessage message);

    CompletableFuture<Void> processMachineUsage(MachineUsageMessage message);

    CompletableFuture<Void> processDeviceStatus(DeviceStatusMessage message);

    CompletableFuture<JsonArray> getAllEvents();

    CompletableFuture<JsonArray> getAllEventsByType(String eventType);

    CompletableFuture<Optional<JsonObject>> getEventById(String eventId);
}