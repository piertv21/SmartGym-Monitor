package com.smartgym.gateway.restcontroller;

import com.smartgym.gateway.service.TokenStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TokenStore tokenStore;

    public AuthController(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateToken(@RequestParam String clientId, @RequestParam String clientSecret) {
        if ("smartgym-client".equals(clientId) && "Smart-Parking".equals(clientSecret)) {
            String token = tokenStore.generateToken();
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}