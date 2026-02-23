package com.smartgym.authservice.application.ports;

import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AuthServiceAPI {

    CompletableFuture<Optional<JsonObject>> authenticate(String username, String password);

    CompletableFuture<Boolean> userExists(String username);

    CompletableFuture<JsonObject> registerLogin(String username);

    CompletableFuture<JsonObject> registerLogout(String username);
}
