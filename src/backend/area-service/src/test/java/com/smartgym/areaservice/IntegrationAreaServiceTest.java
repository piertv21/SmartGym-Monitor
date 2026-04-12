package com.smartgym.areaservice;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationAreaServiceTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Pattern CURRENT_COUNT_PATTERN = Pattern.compile("\"currentCount\"\\s*:\\s*(\\d+)");
    private final String baseUrl = System.getenv().getOrDefault("AREA_SERVICE_IT_BASE_URL", "http://localhost:8086");

    @BeforeAll
    void checkPreconditions() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("SMARTGYM_IT_ENABLED", "true")),
                "Set SMARTGYM_IT_ENABLED=true and start docker compose before running integration tests"
        );
        Assumptions.assumeTrue(isServiceHealthy(), "Area service is not reachable on " + baseUrl);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        HttpResponse<String> response = sendGet("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void getAllAreasReturnsOk() throws Exception {
        HttpResponse<String> response = sendGet("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
    }

    @Test
    void getSeededAreaByIdReturnsOk() throws Exception {
        HttpResponse<String> response = sendGet("/entrance-area");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":\"entrance-area\"")
                || response.body().contains("entrance-area"));
    }

    @Test
    void getAreaByIdReturns404ForUnknownArea() throws Exception {
        HttpResponse<String> response = sendGet("/non-existent-area-" + UUID.randomUUID());

        assertEquals(404, response.statusCode());
    }

    @Test
    void processAreaAccessEntryIncrementsCurrentCount() throws Exception {
        String areaId = "cardio-area";
        String badgeId = "it-badge-" + UUID.randomUUID();

        // Read baseline
        HttpResponse<String> before = sendGet("/" + areaId);
        assertEquals(200, before.statusCode());
        int countBefore = extractCurrentCount(before.body());

        // Send IN access
        String accessPayload = """
                {
                  "deviceId": "it-reader-01",
                  "timeStamp": "2026-04-12T09:00:00Z",
                  "badgeId": "%s",
                  "areaId": "%s",
                  "direction": "IN"
                }
                """.formatted(badgeId, areaId);

        HttpResponse<String> accessResponse = sendPost("/access", accessPayload);
        assertEquals(200, accessResponse.statusCode());

        // Verify count increased
        HttpResponse<String> after = sendGet("/" + areaId);
        assertEquals(200, after.statusCode());
        assertEquals(countBefore + 1, extractCurrentCount(after.body()));

        // Clean up: send OUT
        String exitPayload = """
                {
                  "deviceId": "it-reader-01",
                  "timeStamp": "2026-04-12T09:30:00Z",
                  "badgeId": "%s",
                  "areaId": "%s",
                  "direction": "OUT"
                }
                """.formatted(badgeId, areaId);
        sendPost("/exit", exitPayload);
    }

    @Test
    void processAreaExitDecrementsCurrentCount() throws Exception {
        String areaId = "cardio-area";
        String badgeId = "it-badge-exit-" + UUID.randomUUID();

        // First enter to ensure count > 0
        String accessPayload = """
                {
                  "deviceId": "it-reader-01",
                  "timeStamp": "2026-04-12T10:00:00Z",
                  "badgeId": "%s",
                  "areaId": "%s",
                  "direction": "IN"
                }
                """.formatted(badgeId, areaId);
        sendPost("/access", accessPayload);

        HttpResponse<String> before = sendGet("/" + areaId);
        int countBefore = extractCurrentCount(before.body());

        // Now exit
        String exitPayload = """
                {
                  "deviceId": "it-reader-01",
                  "timeStamp": "2026-04-12T10:30:00Z",
                  "badgeId": "%s",
                  "areaId": "%s",
                  "direction": "OUT"
                }
                """.formatted(badgeId, areaId);

        HttpResponse<String> exitResponse = sendPost("/exit", exitPayload);
        assertEquals(200, exitResponse.statusCode());

        HttpResponse<String> after = sendGet("/" + areaId);
        assertEquals(countBefore - 1, extractCurrentCount(after.body()));
    }

    private int extractCurrentCount(String jsonBody) {
        Matcher matcher = CURRENT_COUNT_PATTERN.matcher(jsonBody);
        assertTrue(matcher.find(), "Expected 'currentCount' in response body: " + jsonBody);
        return Integer.parseInt(matcher.group(1));
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
