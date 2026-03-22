package com.smartgym.authservice;

import com.smartgym.authservice.application.AuthServiceApiImpl;
import com.smartgym.authservice.application.ports.AuthRepository;
import com.smartgym.authservice.model.AuthUser;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class JUnitAuthServiceTest {

    @Test
    void authenticateSuccessWithDomainEntity() {
        AuthRepository repository = new InMemoryAuthRepository(Map.of("ADMIN", new AuthUser("ADMIN", "ADMIN")));
        AuthServiceApiImpl authService = new AuthServiceApiImpl(repository);

        Optional<AuthUser> result = authService.authenticate("ADMIN", "ADMIN").join();

        assertTrue(result.isPresent());
        assertEquals("ADMIN", result.get().getUsername());
    }

    @Test
    void authenticateFailsWithWrongPassword() {
        AuthRepository repository = new InMemoryAuthRepository(Map.of("ADMIN", new AuthUser("ADMIN", "ADMIN")));
        AuthServiceApiImpl authService = new AuthServiceApiImpl(repository);

        Optional<AuthUser> result = authService.authenticate("ADMIN", "WRONG").join();

        assertTrue(result.isEmpty());
    }

    @Test
    void verifyUserExists() {
        AuthRepository repository = new InMemoryAuthRepository(Map.of("ADMIN", new AuthUser("ADMIN", "ADMIN")));
        AuthServiceApiImpl authService = new AuthServiceApiImpl(repository);

        boolean exists = authService.userExists("ADMIN").join();

        assertTrue(exists);
    }

    @Test
    void registerLoginWritesExpectedPayload() {
        AuthRepository repository = new InMemoryAuthRepository(Map.of());
        AuthServiceApiImpl authService = new AuthServiceApiImpl(repository);

        JsonObject payload = authService.registerLogin("ADMIN").join();

        assertEquals("ADMIN", payload.getString("username"));
        assertEquals("login", payload.getString("action"));
        assertNotNull(payload.getLong("timestamp"));
    }

    private static final class InMemoryAuthRepository implements AuthRepository {

        private final Map<String, AuthUser> usersByUsername;

        private InMemoryAuthRepository(Map<String, AuthUser> usersByUsername) {
            this.usersByUsername = usersByUsername;
        }

        @Override
        public CompletableFuture<Optional<AuthUser>> findUserByUsername(String username) {
            return CompletableFuture.completedFuture(Optional.ofNullable(usersByUsername.get(username)));
        }

        @Override
        public CompletableFuture<Void> saveLoginLog(String username, long timestamp) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> saveLogoutLog(String username, long timestamp) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> ensureDefaultAdmin() {
            return CompletableFuture.completedFuture(null);
        }
    }
}
