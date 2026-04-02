package com.smartgym.authservice.application;

import com.smartgym.authservice.application.ports.AuthRepository;
import com.smartgym.authservice.application.ports.AuthServiceAPI;
import com.smartgym.authservice.model.AuthUser;
import io.vertx.core.json.JsonObject;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class AuthServiceApiImpl implements AuthServiceAPI {

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceApiImpl(AuthRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CompletableFuture<Optional<AuthUser>> authenticate(String username, String password) {
        System.out.println("[AuthServiceApiImpl] authenticating user: " + username);
        return repository.findUserByUsername(username)
                .thenApply(optionalUser -> optionalUser.filter(user -> passwordEncoder.matches(password, user.getPassword())));
    }

    @Override
    public CompletableFuture<Boolean> registerUser(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        return repository.saveUser(new AuthUser(username, passwordHash));
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
        String timestamp = LocalDateTime.now().toString();
        return repository.saveLoginLog(username, timestamp)
                .thenApply(v -> new JsonObject()
                        .put("username", username)
                        .put("timestamp", timestamp)
                        .put("action", "login"));
    }

    @Override
    public CompletableFuture<JsonObject> registerLogout(String username) {
        System.out.println("[AuthServiceApiImpl] registering logout for user: " + username);
        String timestamp = LocalDateTime.now().toString();
        return repository.saveLogoutLog(username, timestamp)
                .thenApply(v -> new JsonObject()
                        .put("username", username)
                        .put("timestamp", timestamp)
                        .put("action", "logout"));
    }
}
