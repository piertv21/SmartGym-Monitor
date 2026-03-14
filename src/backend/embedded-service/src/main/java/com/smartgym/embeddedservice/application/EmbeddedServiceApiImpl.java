package com.smartgym.embeddedservice.application;

import com.smartgym.embeddedservice.application.ports.EmbeddedRepository;
import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.TicketMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EmbeddedServiceApiImpl implements EmbeddedServiceAPI {

    private final EmbeddedRepository repository;

    public EmbeddedServiceApiImpl(EmbeddedRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<JsonObject> registerEvent(String eventType, JsonObject payload) {
        System.out.println("[EmbeddedServiceApiImpl] registering event: " + eventType);

        payload.put("EventType", eventType);
        return repository.saveEvent(payload).thenApply(v -> payload);
    }

    @Override
    public CompletableFuture<Optional<JsonObject>> getEventById(String id) {
        return repository.findById(id);
    }

    @Override
    public CompletableFuture<JsonArray> getAllEventsByType(String eventType) {
        return repository.findAllByType(eventType);
    }

    @Override
    public CompletableFuture<JsonArray> getAllEventsByDevice(String deviceId) {
        return repository.findAllByDevice(deviceId);
    }

    @Override
    public CompletableFuture<Void> deleteEvent(String id) {
        return repository.deleteById(id);
    }

    @Override
    public CompletableFuture<JsonArray> getAllEvents() {
        // recupera tutti gli eventi indipendentemente dal tipo
        return repository.findAllByType("*");
    }

    @Override
    public CompletableFuture<TicketMessage> saveNfcPendingPayment(TicketMessage ticketMessage) {
        return repository.saveNfcPendingPayment(ticketMessage);
    }

    @Override
    public CompletableFuture<JsonObject> getPendingPayment() {
        return repository.findPendingPayment()
                .exceptionally(ex -> {
                    System.err.println("❌ Error getPendingPayment: " + ex.getMessage());
                    return new JsonObject().put("pending", false).put("info", "no pending payment");
                });
    }

    @Override
    public CompletableFuture<JsonObject> deletePendingPayment() {
        return repository.deletePendingPayment();
    }

}