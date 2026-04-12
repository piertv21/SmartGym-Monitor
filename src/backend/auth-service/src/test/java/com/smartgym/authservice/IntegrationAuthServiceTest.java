package com.smartgym.authservice;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationAuthServiceTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final String baseUrl = System.getenv().getOrDefault("AUTH_SERVICE_IT_BASE_URL", "http://localhost:8081");

    @BeforeAll
    void checkPreconditions() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("SMARTGYM_IT_ENABLED", "true")),
                "Set SMARTGYM_IT_ENABLED=true and start docker compose before running integration tests"
        );
        Assumptions.assumeTrue(isServiceHealthy(), "Auth service is not reachable on " + baseUrl);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        HttpResponse<String> response = sendGet("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void verifyDefaultAdminUserExists() throws Exception {
        HttpResponse<String> response = sendGet("/login/ADMIN");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"exists\":true"));
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        String payload = """
                { "username": "ADMIN", "password": "ADMIN" }
                """;

        HttpResponse<String> response = sendPost("/login", payload);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("accessToken"));
        assertTrue(response.body().contains("Bearer"));
        assertTrue(response.body().contains("expiresIn"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String payload = """
                { "username": "ADMIN", "password": "WRONG_PASSWORD" }
                """;

        HttpResponse<String> response = sendPost("/login", payload);

        assertEquals(401, response.statusCode());
    }

    @Test
    void verifyNonExistentUserReturns404() throws Exception {
        HttpResponse<String> response = sendGet("/login/NON_EXISTENT_USER_" + UUID.randomUUID());

        assertEquals(404, response.statusCode());
    }

    @Test
    void registerNewUserAndLogin() throws Exception {
        String username = "it-user-" + UUID.randomUUID();
        String password = "test-password-123";

        String registerPayload = """
                { "username": "%s", "password": "%s" }
                """.formatted(username, password);

        HttpResponse<String> registerResponse = sendPost("/register", registerPayload);
        assertEquals(201, registerResponse.statusCode());

        // Verify user exists
        HttpResponse<String> verifyResponse = sendGet("/login/" + username);
        assertEquals(200, verifyResponse.statusCode());
        assertTrue(verifyResponse.body().contains("\"exists\":true"));

        // Login with new user
        String loginPayload = """
                { "username": "%s", "password": "%s" }
                """.formatted(username, password);

        HttpResponse<String> loginResponse = sendPost("/login", loginPayload);
        assertEquals(200, loginResponse.statusCode());
        assertTrue(loginResponse.body().contains("accessToken"));
    }

    @Test
    void registerDuplicateUserReturns409() throws Exception {
        String username = "it-dup-" + UUID.randomUUID();
        String payload = """
                { "username": "%s", "password": "pass123" }
                """.formatted(username);

        HttpResponse<String> first = sendPost("/register", payload);
        assertEquals(201, first.statusCode());

        HttpResponse<String> second = sendPost("/register", payload);
        assertEquals(409, second.statusCode());
    }

    @Test
    void registerWithBlankCredentialsReturns400() throws Exception {
        String payload = """
                { "username": "", "password": "" }
                """;

        HttpResponse<String> response = sendPost("/register", payload);

        assertEquals(400, response.statusCode());
    }

    // ── HTTP helpers ──

    private boolean isServiceHealthy() {
        try {
            HttpResponse<String> response = sendGet("/actuator/health");
            return response.statusCode() == 200 && response.body().contains("UP");
        } catch (Exception ex) {
            return false;
        }
    }

    private HttpResponse<String> sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
