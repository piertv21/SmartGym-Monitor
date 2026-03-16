package com.smartgym.embeddedservice.application.ports;
import com.smartgym.embeddedservice.model.TicketMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EmbeddedRepository {

    CompletableFuture<Void> saveEvent(JsonObject event);

    CompletableFuture<JsonArray> findAllByType(String type);

    CompletableFuture<JsonArray> findAllByDevice(String deviceId);

    CompletableFuture<Optional<JsonObject>> findById(String id);

    CompletableFuture<Void> deleteById(String id);

    CompletableFuture<TicketMessage> saveNfcPendingPayment(TicketMessage plate);

    CompletableFuture<JsonObject> findPendingPayment();

    CompletableFuture<JsonObject> deletePendingPayment();

}
