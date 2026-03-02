package com.smartgym.authservice.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password";

    public boolean userExists(String id) {
        return id.equals("1") || id.equals("admin");
    }

    public String login(String username, String password) {
        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            return "jwt-token-" + UUID.randomUUID().toString();
        }
        throw new RuntimeException("Invalid credentials");
    }

}
