package com.smartgym.gateway.restcontroller;

import com.smartgym.gateway.service.TokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TokenStore tokenStore;
    private final String allowedClientId;
    private final String allowedClientSecret;

    public AuthController(
            TokenStore tokenStore,
            @Value("${GATEWAY_CLIENT_ID:smartgym-client}") String allowedClientId,
            @Value("${GATEWAY_CLIENT_SECRET:smartgym-secret}") String allowedClientSecret
    ) {
        this.tokenStore = tokenStore;
        this.allowedClientId = allowedClientId;
        this.allowedClientSecret = allowedClientSecret;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateToken(@RequestBody GenerateTokenRequest request) {
        if (request != null
                && allowedClientId.equals(request.clientId())
                && allowedClientSecret.equals(request.clientSecret())) {
            String token = tokenStore.generateToken();
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    public record GenerateTokenRequest(String clientId, String clientSecret) {}
}