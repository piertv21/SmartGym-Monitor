package com.smartgym.analyticsservice.application.ports;

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsRepository {

    CompletableFuture<Void> saveEvent(JsonObject event);


    CompletableFuture<List<JsonObject>> findEventsByType(String eventType);

    CompletableFuture<List<JsonObject>> findEventsByTypeAndDate(String eventType, String date);

    CompletableFuture<List<JsonObject>> findEventsByTypeAndDateRange(String eventType, String from, String to);


    CompletableFuture<List<JsonObject>> findEventsByTypeAndMonth(String eventType, String month);
}