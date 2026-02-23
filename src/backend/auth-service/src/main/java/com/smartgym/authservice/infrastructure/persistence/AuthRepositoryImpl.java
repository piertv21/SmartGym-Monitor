package com.smartgym.authservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.smartgym.authservice.application.ports.AuthRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AuthRepositoryImpl implements AuthRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuthRepositoryImpl.class);
    private static final String USERS_COLLECTION = "users";
    private static final String LOGS_COLLECTION = "logs";

    private final MongoCollection<Document> usersCollection;
    private final MongoCollection<Document> logsCollection;

    public AuthRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("authservicedb");
        this.usersCollection = database.getCollection(USERS_COLLECTION);
        this.logsCollection = database.getCollection(LOGS_COLLECTION);
        initializeIndexes();
    }

    private void initializeIndexes() {
        try {
            usersCollection.createIndex(
                    Indexes.ascending("username"),
                    new IndexOptions().unique(true)
            );

            logsCollection.createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("username"),
                            Indexes.descending("timestamp")
                    )
            );

            logger.info("✅ MongoDB indexes created successfully");
        } catch (Exception e) {
            logger.error("❌ Failed to create indexes: {}", e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Optional<JsonObject>> findUserByUsername(String username) {
        CompletableFuture<Optional<JsonObject>> future = new CompletableFuture<>();

        try {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Invalid username");
            }

            Document query = new Document("username", username);
            Document result = usersCollection.find(query).first();

            logger.debug("User lookup for '{}': {}", username, result != null ? "found" : "not found");

            future.complete(Optional.ofNullable(
                    result != null ? new JsonObject(result.toJson()) : null
            ));
        } catch (Exception e) {
            logger.error("Error finding user '{}': {}", username, e.getMessage());
            future.completeExceptionally(
                    new RuntimeException("Failed to find user: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> saveLoginLog(String username, long timestamp) {
        return saveLog(username, timestamp, "LOGIN");
    }

    @Override
    public CompletableFuture<Void> saveLogoutLog(String username, long timestamp) {
        return saveLog(username, timestamp, "LOGOUT");
    }

    private CompletableFuture<Void> saveLog(String username, long timestamp, String action) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Invalid username");
            }

            Document logDoc = new Document()
                    .append("username", username)
                    .append("action", action)
                    .append("timestamp", timestamp);

            logsCollection.insertOne(logDoc);
            logger.debug("Log saved: {} for user '{}'", action, username);

            future.complete(null);
        } catch (Exception e) {
            logger.error("Failed to save {} log for '{}': {}", action, username, e.getMessage());
            future.completeExceptionally(
                    new RuntimeException("Failed to save " + action + " log: " + e.getMessage(), e));
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> initializeUsers() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            long count = usersCollection.countDocuments();
            if (count > 0) {
                logger.info("Users collection already contains {} documents", count);
                future.complete(null);
                return future;
            }

            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("ADMIN_LIST.json");

            if (is == null) {
                throw new RuntimeException("ADMIN_LIST.json not found in resources");
            }

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonArray adminList = new JsonArray(json);

            int inserted = 0;
            for (Object obj : adminList) {
                JsonObject user = (JsonObject) obj;
                Document doc = Document.parse(user.encode());

                try {
                    usersCollection.insertOne(doc);
                    inserted++;
                } catch (Exception e) {
                    logger.warn("Failed to insert user '{}': {}",
                            user.getString("username"), e.getMessage());
                }
            }

            logger.info("✅ Users initialized: {} documents inserted", inserted);
            future.complete(null);
        } catch (Exception e) {
            logger.error("❌ Failed to initialize users: {}", e.getMessage());
            future.completeExceptionally(
                    new RuntimeException("Failed to initialize users: " + e.getMessage(), e));
        }

        return future;
    }
}

