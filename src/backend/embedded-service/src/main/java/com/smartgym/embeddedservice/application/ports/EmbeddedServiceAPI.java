package com.smartgym.embeddedservice.application.ports;

import com.smartgym.embeddedservice.model.TicketMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface EmbeddedServiceAPI {

    CompletableFuture<JsonObject> registerEvent(String eventType, JsonObject payload);

    CompletableFuture<Optional<JsonObject>> getEventById(String id);

    CompletableFuture<JsonArray> getAllEventsByType(String eventType);

    CompletableFuture<JsonArray> getAllEventsByDevice(String deviceId);

    CompletableFuture<Void> deleteEvent(String id);

    CompletableFuture<JsonArray> getAllEvents();

    CompletableFuture<TicketMessage> saveNfcPendingPayment(TicketMessage ticketMessage);

    CompletableFuture<JsonObject> getPendingPayment();

    CompletableFuture<JsonObject> deletePendingPayment();
}
