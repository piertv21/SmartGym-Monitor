package com.smartgym.authservice.application.ports;

import com.smartgym.authservice.ddd.Repository;
import com.smartgym.authservice.model.AuthUser;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AuthRepository extends Repository {
    CompletableFuture<Optional<AuthUser>> findUserByUsername(String username);
    CompletableFuture<Boolean> saveUser(AuthUser user);
    CompletableFuture<Void> saveLoginLog(String username, String timestamp);
    CompletableFuture<Void> saveLogoutLog(String username, String timestamp);
    CompletableFuture<Void> ensureDefaultAdmin();
}
