package com.smartgym.authservice.application;

import com.smartgym.authservice.application.ports.AuthRepository;
import com.smartgym.authservice.application.ports.AuthServiceAPI;
import com.smartgym.authservice.model.AuthUser;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class AuthServiceApiImpl implements AuthServiceAPI {

    private final AuthRepository repository;

    public AuthServiceApiImpl(AuthRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Optional<AuthUser>> authenticate(String username, String password) {
        System.out.println("[AuthServiceApiImpl] authenticating user: " + username);
        return repository.findUserByUsername(username)
                .thenApply(optionalUser -> optionalUser.filter(user -> user.matchesPassword(password)));
    }

    @Override
    public CompletableFuture<Boolean> userExists(String username) {
        System.out.println("[AuthServiceApiImpl] checking if user exists: " + username);
        return repository.findUserByUsername(username)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<JsonObject> registerLogin(String username) {
        System.out.println("[AuthServiceApiImpl] registering login for user: " + username);
        long timestamp = System.currentTimeMillis();
        return repository.saveLoginLog(username, timestamp)
                .thenApply(v -> new JsonObject()
                        .put("username", username)
                        .put("timestamp", timestamp)
                        .put("action", "login"));
    }

    @Override
    public CompletableFuture<JsonObject> registerLogout(String username) {
        System.out.println("[AuthServiceApiImpl] registering logout for user: " + username);
        long timestamp = System.currentTimeMillis();
        return repository.saveLogoutLog(username, timestamp)
                .thenApply(v -> new JsonObject()
                        .put("username", username)
                        .put("timestamp", timestamp)
                        .put("action", "logout"));
    }
}
