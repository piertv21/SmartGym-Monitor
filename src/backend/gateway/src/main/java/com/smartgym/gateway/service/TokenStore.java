package com.smartgym.gateway.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final Set<String> validTokens = ConcurrentHashMap.newKeySet();

    public String generateToken() {
        String token = UUID.randomUUID().toString();
        validTokens.add(token);
        return token;
    }

    public boolean isValid(String token) {
        return validTokens.contains(token);
    }

    public void revoke(String token) {
        validTokens.remove(token);
    }
}
