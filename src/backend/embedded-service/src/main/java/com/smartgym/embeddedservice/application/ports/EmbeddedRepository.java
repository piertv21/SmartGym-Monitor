package com.smartgym.embeddedservice.application.ports;

import com.smartgym.embeddedservice.ddd.Repository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public interface EmbeddedRepository extends Repository {

    CompletableFuture<Void> saveEvent(JsonObject event);


    CompletableFuture<JsonArray> findLatestDeviceStatuses();
}