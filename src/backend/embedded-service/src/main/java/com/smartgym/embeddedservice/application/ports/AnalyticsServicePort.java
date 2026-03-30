package com.smartgym.embeddedservice.application.ports;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public interface AnalyticsServicePort {

    CompletableFuture<Void> ingestEvent(JsonObject event);
}
