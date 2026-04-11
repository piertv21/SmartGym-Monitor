package com.smartgym.analyticsservice.application.ports;

import com.smartgym.analyticsservice.ddd.Repository;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsRepository extends Repository {

    CompletableFuture<Void> saveEvent(JsonObject event);


    CompletableFuture<List<JsonObject>> findEventsByType(String eventType);

    CompletableFuture<List<JsonObject>> findEventsByTypeAndDate(String eventType, String date);

    CompletableFuture<List<JsonObject>> findEventsByTypeAndDateRange(String eventType, String from, String to);
}