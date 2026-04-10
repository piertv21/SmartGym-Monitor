package com.smartgym.analyticsservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import io.vertx.core.json.JsonObject;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private static final String DATABASE = "analyticsservicedb";
    private static final String EVENTS_COLLECTION = "analyticsEvents";
    private static final ZoneId ANALYTICS_ZONE = ZoneId.of("Europe/Rome");

    private final MongoCollection<Document> eventsCollection;

    public AnalyticsRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase(DATABASE);
        this.eventsCollection = database.getCollection(EVENTS_COLLECTION);
    }

    @Override
    public CompletableFuture<Void> saveEvent(JsonObject event) {
        return CompletableFuture.runAsync(() -> eventsCollection.insertOne(toDocument(event)));
    }


    @Override
    public CompletableFuture<List<JsonObject>> findEventsByType(String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonObject> result = new ArrayList<>();
            for (Document document : eventsCollection.find(new Document("eventType", eventType))) {
                result.add(fromDocument(document));
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<List<JsonObject>> findEventsByTypeAndDate(String eventType, String date) {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonObject> result = new ArrayList<>();
            for (Document document : eventsCollection.find(new Document("eventType", eventType).append("eventDate", date))) {
                result.add(fromDocument(document));
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<List<JsonObject>> findEventsByTypeAndDateRange(String eventType, String from, String to) {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonObject> result = new ArrayList<>();
            Bson filter = Filters.and(
                    Filters.eq("eventType", eventType),
                    Filters.gte("eventDate", from),
                    Filters.lte("eventDate", to)
            );
            for (Document document : eventsCollection.find(filter)) {
                result.add(fromDocument(document));
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<List<JsonObject>> findEventsByTypeAndMonth(String eventType, String month) {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonObject> result = new ArrayList<>();
            for (Document document : eventsCollection.find(new Document("eventType", eventType).append("eventMonth", month))) {
                result.add(fromDocument(document));
            }
            return result;
        });
    }

    private Document toDocument(JsonObject event) {
        JsonObject payload = event.getJsonObject("payload", new JsonObject());
        String timestamp = extractTimestamp(event, payload);
        LocalDate date = parseDate(timestamp);
        YearMonth month = YearMonth.from(date);

        return new Document()
                .append("eventType", event.getString("eventType"))
                .append("eventDate", date.toString())
                .append("eventMonth", month.toString())
                .append("timestamp", timestamp)
                .append("payload", Document.parse(payload.encode()));
    }

    private JsonObject fromDocument(Document document) {
        JsonObject event = new JsonObject()
                .put("eventType", document.getString("eventType"))
                .put("eventDate", document.getString("eventDate"))
                .put("eventMonth", document.getString("eventMonth"))
                .put("timestamp", document.getString("timestamp"));

        Object payload = document.get("payload");
        if (payload instanceof Document payloadDoc) {
            event.put("payload", new JsonObject(payloadDoc.toJson()));
        } else {
            event.put("payload", new JsonObject());
        }
        return event;
    }

    private String extractTimestamp(JsonObject event, JsonObject payload) {
        String inPayload = payload.getString("timeStamp", payload.getString("timestamp"));
        if (inPayload != null && !inPayload.isBlank()) {
            return inPayload;
        }
        String topLevel = event.getString("timeStamp", event.getString("timestamp"));
        if (topLevel != null && !topLevel.isBlank()) {
            return topLevel;
        }
        throw new IllegalArgumentException("Event requires timeStamp or timestamp");
    }

    private LocalDate parseDate(String timestamp) {
        try {
            return Instant.parse(timestamp).atZone(ANALYTICS_ZONE).toLocalDate();
        } catch (Exception ignored) {
            return LocalDate.parse(timestamp.substring(0, 10));
        }
    }
}