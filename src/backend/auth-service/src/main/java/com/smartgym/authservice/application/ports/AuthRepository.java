package com.smartgym.authservice.application.ports;

import com.smartgym.authservice.model.AuthUser;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AuthRepository {
    CompletableFuture<Optional<AuthUser>> findUserByUsername(String username);
    CompletableFuture<Boolean> saveUser(AuthUser user);
    CompletableFuture<Void> saveLoginLog(String username, long timestamp);
    CompletableFuture<Void> saveLogoutLog(String username, long timestamp);
    CompletableFuture<Void> ensureDefaultAdmin();
}
