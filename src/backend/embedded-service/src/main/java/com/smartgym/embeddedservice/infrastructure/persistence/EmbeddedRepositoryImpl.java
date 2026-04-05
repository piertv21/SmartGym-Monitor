package com.smartgym.embeddedservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.embeddedservice.application.ports.EmbeddedRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class EmbeddedRepositoryImpl implements EmbeddedRepository {

    private static final String DATABASE = "embeddedservicedb";
    private static final String COLLECTION = "embeddedservicedbcollection";
    private static final String DEVICE_STATUS_EVENT = "DEVICE_STATUS";

    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public EmbeddedRepositoryImpl(MongoClient mongoClient) {
        this.database = mongoClient.getDatabase(DATABASE);
        this.collection = database.getCollection(COLLECTION);
    }

    @Override
    public CompletableFuture<Void> saveEvent(JsonObject event) {
        return CompletableFuture.runAsync(() -> {
            Document document = Document.parse(event.encode());
            collection.insertOne(document);
        });
    }

    @Override
    public CompletableFuture<Optional<JsonObject>> findEventById(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            Document document = collection.find(new Document("eventId", eventId)).first();

            if (document == null) {
                return Optional.empty();
            }

            return Optional.of(new JsonObject(document.toJson()));
        });
    }

    @Override
    public CompletableFuture<JsonArray> findAllEvents() {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonObject> result = new ArrayList<>();

            for (Document document : collection.find()) {
                result.add(new JsonObject(document.toJson()));
            }

            return new JsonArray(result);
        });
    }

    @Override
    public CompletableFuture<JsonArray> findAllEventsByType(String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonObject> result = new ArrayList<>();

            for (Document document : collection.find(new Document("eventType", eventType))) {
                result.add(new JsonObject(document.toJson()));
            }

            return new JsonArray(result);
        });
    }

    @Override
    public CompletableFuture<JsonArray> findLatestDeviceStatuses() {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonObject> statuses = new ArrayList<>();
            Set<String> processedDevices = new HashSet<>();

            for (Document document : collection.find(new Document("eventType", DEVICE_STATUS_EVENT))
                    .sort(new Document("timeStamp", -1).append("_id", -1))) {
                String deviceId = document.getString("deviceId");
                if (deviceId == null || deviceId.isBlank() || processedDevices.contains(deviceId)) {
                    continue;
                }

                Object payload = document.get("payload");
                JsonObject status = payload instanceof Document payloadDoc
                        ? new JsonObject(payloadDoc.toJson())
                        : new JsonObject()
                        .put("deviceId", deviceId)
                        .put("timeStamp", document.getString("timeStamp"));

                statuses.add(status);
                processedDevices.add(deviceId);
            }

            return new JsonArray(statuses);
        });
    }
}