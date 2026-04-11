package com.smartgym.embeddedservice.application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public interface EmbeddedRepository {

    CompletableFuture<Void> saveEvent(JsonObject event);


    CompletableFuture<JsonArray> findLatestDeviceStatuses();
}