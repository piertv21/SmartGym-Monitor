package com.smartgym.embeddedservice.application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EmbeddedRepository {

    CompletableFuture<Void> saveEvent(JsonObject event);

    CompletableFuture<Optional<JsonObject>> findEventById(String eventId);

    CompletableFuture<JsonArray> findAllEvents();

    CompletableFuture<JsonArray> findAllEventsByType(String eventType);

    CompletableFuture<JsonArray> findLatestDeviceStatuses();
}