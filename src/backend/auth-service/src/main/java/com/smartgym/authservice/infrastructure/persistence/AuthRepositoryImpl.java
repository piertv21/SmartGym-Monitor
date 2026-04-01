package com.smartgym.authservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.smartgym.authservice.application.ports.AuthRepository;
import com.smartgym.authservice.model.AuthUser;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AuthRepositoryImpl implements AuthRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuthRepositoryImpl.class);
    private static final String USERS_COLLECTION = "users";
    private static final String LOGS_COLLECTION = "logs";

    private final MongoCollection<Document> usersCollection;
    private final MongoCollection<Document> logsCollection;
    private final PasswordEncoder passwordEncoder;
    private final String defaultAdminUsername;
    private final String defaultAdminPassword;

    public AuthRepositoryImpl(
            MongoClient mongoClient,
            PasswordEncoder passwordEncoder,
            String defaultAdminUsername,
            String defaultAdminPassword
    ) {
        MongoDatabase database = mongoClient.getDatabase("authservicedb");
        this.usersCollection = database.getCollection(USERS_COLLECTION);
        this.logsCollection = database.getCollection(LOGS_COLLECTION);
        this.passwordEncoder = passwordEncoder;
        this.defaultAdminUsername = defaultAdminUsername;
        this.defaultAdminPassword = defaultAdminPassword;
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
    public CompletableFuture<Optional<AuthUser>> findUserByUsername(String username) {
        CompletableFuture<Optional<AuthUser>> future = new CompletableFuture<>();

        try {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Invalid username");
            }

            Document query = new Document("username", username);
            Document result = usersCollection.find(query).first();

            logger.debug("User lookup for '{}': {}", username, result != null ? "found" : "not found");

            future.complete(Optional.ofNullable(result).map(this::toDomain));
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
    public CompletableFuture<Boolean> saveUser(AuthUser user) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
                throw new IllegalArgumentException("Invalid user payload");
            }

            Document existing = usersCollection.find(new Document("username", user.getUsername())).first();
            if (existing != null) {
                future.complete(false);
                return future;
            }

            usersCollection.insertOne(toDocument(user));
            future.complete(true);
        } catch (Exception e) {
            logger.error("Failed to save user '{}': {}", user != null ? user.getUsername() : "unknown", e.getMessage());
            future.completeExceptionally(new RuntimeException("Failed to save user: " + e.getMessage(), e));
        }

        return future;
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
    public CompletableFuture<Void> ensureDefaultAdmin() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            Document existingAdmin = usersCollection.find(new Document("username", defaultAdminUsername)).first();
            if (existingAdmin != null) {
                logger.info("Default admin '{}' already exists", defaultAdminUsername);
                future.complete(null);
                return future;
            }

            String passwordToStore = defaultAdminPassword.startsWith("$2")
                    ? defaultAdminPassword
                    : passwordEncoder.encode(defaultAdminPassword);

            AuthUser defaultAdmin = new AuthUser(defaultAdminUsername, passwordToStore);
            usersCollection.insertOne(toDocument(defaultAdmin));
            logger.info("✅ Default admin '{}' initialized", defaultAdminUsername);
            future.complete(null);
        } catch (Exception e) {
            logger.error("❌ Failed to initialize default admin: {}", e.getMessage());
            future.completeExceptionally(
                    new RuntimeException("Failed to initialize default admin: " + e.getMessage(), e));
        }

        return future;
    }

    private AuthUser toDomain(Document document) {
        return new AuthUser(document.getString("username"), document.getString("password"));
    }

    private Document toDocument(AuthUser user) {
        return new Document("username", user.getUsername())
                .append("password", user.getPassword());
    }
}
