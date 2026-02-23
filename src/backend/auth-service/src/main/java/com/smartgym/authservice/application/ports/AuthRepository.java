package com.smartgym.authservice.application.ports;

import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AuthRepository {
    CompletableFuture<Optional<JsonObject>> findUserByUsername(String username);
    CompletableFuture<Void> saveLoginLog(String username, long timestamp);
    CompletableFuture<Void> saveLogoutLog(String username, long timestamp);
    CompletableFuture<Void> initializeUsers();
}
