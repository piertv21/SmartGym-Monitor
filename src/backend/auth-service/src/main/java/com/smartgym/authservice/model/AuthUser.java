package com.smartgym.authservice.model;

import java.util.Objects;

public class AuthUser {

    private final String username;
    private final String password;

    public AuthUser(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthUser authUser)) {
            return false;
        }
        return Objects.equals(username, authUser.username) && Objects.equals(password, authUser.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}

