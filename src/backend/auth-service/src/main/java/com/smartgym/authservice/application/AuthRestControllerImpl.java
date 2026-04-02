package com.smartgym.authservice.application;

import com.smartgym.authservice.application.ports.AuthRestController;
import com.smartgym.authservice.application.ports.AuthServiceAPI;
import com.smartgym.authservice.model.LoginMessage;
import com.smartgym.authservice.model.LogoutMessage;
import com.smartgym.authservice.model.RegisterMessage;
import com.smartgym.authservice.service.JwtTokenService;
import io.vertx.core.json.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
public class AuthRestControllerImpl implements AuthRestController {

    private final AuthServiceAPI authService;
    private final JwtTokenService jwtTokenService;

    public AuthRestControllerImpl(AuthServiceAPI authService, JwtTokenService jwtTokenService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
    }
    @PostMapping("/login")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> handleLogin(@RequestBody LoginMessage credentials) {
        System.out.println("[AuthRestControllerImpl] handling login request: " + credentials);
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        if (username == null || password == null) {
            JsonObject error = new JsonObject().put("error", "Username e password richiesti");
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
        }

        return authService.authenticate(username, password)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        JsonObject error = new JsonObject().put("error", "Credenziali non valide");
                        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
                    }
                    return authService.registerLogin(username)
                            .thenApply(loginLog -> {
                                String token = jwtTokenService.generateAccessToken(optionalUser.get().getUsername());
                                JsonObject payload = new JsonObject()
                                        .put("accessToken", token)
                                        .put("tokenType", "Bearer")
                                        .put("expiresIn", jwtTokenService.getAccessTokenTtlSeconds());
                                return ResponseEntity.ok(payload);
                            });
                })
                .exceptionally(ex -> {
                    JsonObject error = new JsonObject().put("error", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });
    }

    @PostMapping("/register")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> handleRegister(@RequestBody RegisterMessage registerMessage) {
        String username = registerMessage != null ? registerMessage.getUsername() : null;
        String password = registerMessage != null ? registerMessage.getPassword() : null;

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            JsonObject error = new JsonObject().put("error", "Username e password richiesti");
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
        }

        return authService.registerUser(username, password)
                .thenApply(created -> {
                    if (!created) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(new JsonObject().put("error", "Username gia esistente"));
                    }

                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(new JsonObject().put("message", "Utente registrato"));
                })
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new JsonObject().put("error", ex.getMessage())));
    }

    @GetMapping("/login/{username}")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> handleVerifyUser(@PathVariable String username) {
        System.out.println("[AuthRestControllerImpl] verifying user: " + username);
        return authService.userExists(username)
                .thenApply(exists -> {
                    if (exists) {
                        JsonObject response = new JsonObject()
                                .put("username", username)
                                .put("exists", true);
                        return ResponseEntity.ok(response);
                    } else {
                        JsonObject error = new JsonObject().put("error", "Utente non trovato");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    }
                })
                .exceptionally(ex -> {
                    JsonObject error = new JsonObject().put("error", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });
    }


    @PostMapping("/logout")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> handleLogout(@RequestBody LogoutMessage payload) {
        System.out.println("[AuthRestControllerImpl] handling logout request");
        String username = payload.getUsername();
        System.out.println("[AuthRestControllerImpl] logging out user: " + username);

        if (username == null) {
            JsonObject error = new JsonObject().put("error", "Username richiesto");
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
        }

        return authService.registerLogout(username)
                .thenApply(logoutLog -> {
                    JsonObject response = new JsonObject()
                            .put("message", "Logout effettuato")
                            .put("timestamp", logoutLog.getString("timestamp"));
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    JsonObject error = new JsonObject().put("error", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                });
    }
}
