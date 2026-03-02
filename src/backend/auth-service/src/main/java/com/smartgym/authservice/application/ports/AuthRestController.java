package com.smartgym.authservice.application.ports;

import com.smartgym.authservice.model.LoginMessage;
import com.smartgym.authservice.model.LogoutMessage;
import io.vertx.core.json.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
@RestController
public interface AuthRestController {

    @PostMapping("/login")
    CompletableFuture<ResponseEntity<JsonObject>> handleLogin(@RequestBody LoginMessage credentials);

    @GetMapping("/login/{username}")
    CompletableFuture<ResponseEntity<JsonObject>> handleVerifyUser(@PathVariable String username);

    @PostMapping("/logout")
    CompletableFuture<ResponseEntity<JsonObject>> handleLogout(@RequestBody LogoutMessage message);
}
