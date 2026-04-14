package com.smartgym.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationGatewayTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final String baseUrl =
            System.getenv().getOrDefault("GATEWAY_IT_BASE_URL", "http://localhost:8080");
    private final String authUsername =
            System.getenv().getOrDefault("GATEWAY_IT_AUTH_USERNAME", "ADMIN");
    private final String authPassword =
            System.getenv().getOrDefault("GATEWAY_IT_AUTH_PASSWORD", "ADMIN");

    @BeforeAll
    void checkPreconditions() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("SMARTGYM_IT_ENABLED", "true")),
                "Set SMARTGYM_IT_ENABLED=true and start docker compose before running integration tests");
        Assumptions.assumeTrue(isServiceHealthy(), "Gateway is not reachable on " + baseUrl);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        HttpResponse<String> response = sendGet("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void generateTokenWithValidClientCredentials() throws Exception {
        String token = loginAndGetToken();
        assertNotNull(token);
        assertTrue(token.length() > 20, "Token should be a meaningful JWT string");
    }

    @Test
    void loginEndpointBypassesAuthFilter() throws Exception {
        // POST /auth-service/login is a public endpoint — should not get 401 for missing token
        String body =
                """
                { "username": "%s", "password": "%s" }
                """
                        .formatted(authUsername, authPassword);

        HttpResponse<String> response = sendPost("/auth-service/login", body);

        // Should be 200 (valid credentials) or 401 (wrong credentials), but NOT 401 for missing
        // token
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("accessToken"));
    }

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        // GET /area-service/ is a protected route — must return 401 without Authorization header
        HttpResponse<String> response = sendGet("/area-service/");

        assertEquals(401, response.statusCode());
    }

    @Test
    void protectedRouteWithValidTokenReturnsOk() throws Exception {
        String token = loginAndGetToken();

        HttpResponse<String> response = sendGetWithAuth("/area-service/", token);

        assertEquals(200, response.statusCode());
        assertTrue(
                response.body().startsWith("["), "Expected areas array, got: " + response.body());
    }

    @Test
    void protectedRouteWithInvalidTokenReturns401() throws Exception {
        HttpResponse<String> response = sendGetWithAuth("/area-service/", "invalid.jwt.token");

        assertEquals(401, response.statusCode());
    }

    @Test
    void loginWithWrongCredentialsThroughGatewayReturns401() throws Exception {
        String body =
                """
                { "username": "ADMIN", "password": "WRONG_PASSWORD" }
                """;

        HttpResponse<String> response = sendPost("/auth-service/login", body);

        assertEquals(401, response.statusCode());
    }

    // ── Helpers ──

    private String loginAndGetToken() throws Exception {
        String body =
                """
                { "username": "%s", "password": "%s" }
                """
                        .formatted(authUsername, authPassword);

        HttpResponse<String> response = sendPost("/auth-service/login", body);
        assertEquals(200, response.statusCode(), "Login failed: " + response.body());
        assertTrue(response.body().contains("accessToken"));

        // Extract token — simple regex approach
        String respBody = response.body();
        int tokenStart = respBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int tokenEnd = respBody.indexOf("\"", tokenStart);
        return respBody.substring(tokenStart, tokenEnd);
    }

    private boolean isServiceHealthy() {
        try {
            HttpResponse<String> response = sendGet("/actuator/health");
            return response.statusCode() == 200 && response.body().contains("UP");
        } catch (Exception ex) {
            return false;
        }
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendGetWithAuth(String path, String bearerToken) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + bearerToken)
                        .GET()
                        .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String jsonBody) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
