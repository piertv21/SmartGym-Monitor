package com.smartgym.embeddedservice.infrastracture.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.embeddedservice.application.ports.EmbeddedRepository;
import com.smartgym.embeddedservice.model.TicketMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class EmbeddedRepositoryImpl implements EmbeddedRepository {

    private static final String COLLECTION = "embeddedservicedbcollection";
    private static final String COLLECTIONPAYMENT = "embeddedservicedbcollectionpayment";
    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> collectionpayment;
    MongoDatabase database;

    public EmbeddedRepositoryImpl(MongoClient mongoClient) {
        this.database = mongoClient.getDatabase("embeddedservicedb");
        this.collection = database.getCollection(COLLECTION);
        this.collectionpayment = database.getCollection(COLLECTIONPAYMENT);
    }

    @Override
    public CompletableFuture<Void> saveEvent(JsonObject event) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (event == null || !event.containsKey("deviceId")) {
                System.out.println("Illegal DeviceId or event");
                throw new IllegalArgumentException("Invalid event data");
            }

            Document doc = Document.parse(event.encode());
            doc.put("createdAt", System.currentTimeMillis());

            collection.insertOne(doc);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(
                    new RuntimeException("Failed to save event: " + e.getMessage(), e));
        }
        return future;
    }

    @Override
    public CompletableFuture<JsonArray> findAllByType(String type) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();

        try {
            Bson query = type.equals("*")
                    ? new Document()
                    : new Document("type", type);

            JsonArray results = new JsonArray();
            for (Document doc : collection.find(query)) {
                results.add(new JsonObject(doc.toJson()));
            }

            future.complete(results);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<JsonArray> findAllByDevice(String deviceId) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();

        try {
            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalArgumentException("Invalid deviceId");
            }

            JsonArray results = new JsonArray();
            for (Document doc : collection.find(new Document("deviceId", deviceId))) {
                results.add(new JsonObject(doc.toJson()));
            }

            future.complete(results);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<Optional<JsonObject>> findById(String id) {
        CompletableFuture<Optional<JsonObject>> future = new CompletableFuture<>();

        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Invalid id");
            }

            Document result = collection.find(new Document("_id", id)).first();
            future.complete(Optional.ofNullable(result != null ? new JsonObject(result.toJson()) : null));
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> deleteById(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Invalid id");
            }

            collection.deleteOne(new Document("_id", id));
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private CompletableFuture<TicketMessage> savePayment(JsonObject json) {
        CompletableFuture<TicketMessage> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                collectionpayment.deleteMany(new Document("type", "nfc_pending_payment"));
                Document doc = Document.parse(json.encode());
                String formatted = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                doc.put("Initiated_at", formatted);
                collectionpayment.insertOne(doc);
                future.complete(null);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public CompletableFuture<TicketMessage> saveNfcPendingPayment(TicketMessage ticketMessage) {
        JsonObject doc = JsonObject.mapFrom(ticketMessage)
                .put("type", "TERMINATED_TICKET");

        System.out.println("Saving NFC pending payment: " + doc.encodePrettily());

        return savePayment(doc);
    }

    @Override
    public CompletableFuture<JsonObject> deletePendingPayment() {

        return CompletableFuture.supplyAsync(() -> {
            Document doc = collectionpayment.find().first();
            if (doc == null) {
                return new JsonObject()
                        .put("deleted", false)
                        .put("pending", false);
            }
            collectionpayment.deleteOne(new Document("_id", doc.getObjectId("_id")));
            return new JsonObject()
                    .put("deleted", true)
                    .put("pending", false)
                    .put("removed_id", doc.getObjectId("_id").toString());
        });
    }

    @Override
    public CompletableFuture<JsonObject> findPendingPayment() {

        return CompletableFuture.supplyAsync(() -> {

            Document doc = collectionpayment.find().first();

            if (doc == null) {
                return new JsonObject().put("pending", false);
            }

            JsonObject json = new JsonObject(doc.toJson());
            json.put("pending", true);

            return json;
        });
    }
}
