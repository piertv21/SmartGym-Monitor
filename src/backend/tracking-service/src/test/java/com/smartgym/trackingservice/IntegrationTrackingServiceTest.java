package com.smartgym.trackingservice;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

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
class IntegrationTrackingServiceTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Pattern GYM_COUNT_PATTERN = Pattern.compile("\"gymCount\"\\s*:\\s*(\\d+)");
    private final String baseUrl = System.getenv().getOrDefault("TRACKING_SERVICE_IT_BASE_URL", "http://localhost:8087");

    @BeforeAll
    void checkPreconditions() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("SMARTGYM_IT_ENABLED", "true")),
                "Set SMARTGYM_IT_ENABLED=true and start docker compose before running integration tests"
        );
        Assumptions.assumeTrue(isServiceHealthy(), "Tracking service is not reachable on " + baseUrl);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        HttpResponse<String> response = sendGet("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void startAndEndSessionUpdatesGymCount() throws Exception {
        String badgeId = "badge-" + UUID.randomUUID();

        HttpResponse<String> countBeforeStart = sendGet("/count");
        assertEquals(200, countBeforeStart.statusCode());
        int initialGymCount = extractGymCount(countBeforeStart.body());

        HttpResponse<String> startResponse = sendPost(
                "/start-session",
                "{\"badgeId\":\"" + badgeId + "\"}"
        );
        assertEquals(200, startResponse.statusCode());

        HttpResponse<String> countAfterStart = sendGet("/count");
        assertEquals(200, countAfterStart.statusCode());
        assertEquals(initialGymCount + 1, extractGymCount(countAfterStart.body()));

        HttpResponse<String> endResponse = sendPost(
                "/end-session",
                "{\"badgeId\":\"" + badgeId + "\"}"
        );
        assertEquals(200, endResponse.statusCode());

        HttpResponse<String> countAfterEnd = sendGet("/count");
        assertEquals(200, countAfterEnd.statusCode());
        assertEquals(initialGymCount, extractGymCount(countAfterEnd.body()));
    }

    @Test
    void getActiveSessionsReturnsArray() throws Exception {
        HttpResponse<String> response = sendGet("/active-sessions");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
    }

    @Test
    void doubleStartSessionForSameBadgeFails() throws Exception {
        String badgeId = "badge-double-" + UUID.randomUUID();
        String payload = "{\"badgeId\":\"" + badgeId + "\"}";

        HttpResponse<String> firstStart = sendPost("/start-session", payload);
        assertEquals(200, firstStart.statusCode());

        // Second start with same badge should fail
        HttpResponse<String> secondStart = sendPost("/start-session", payload);
        assertTrue(secondStart.statusCode() >= 400,
                "Double start should fail, got status=" + secondStart.statusCode());

        // Cleanup
        sendPost("/end-session", payload);
    }

    @Test
    void endSessionWithoutStartFails() throws Exception {
        String badgeId = "badge-nostart-" + UUID.randomUUID();
        String payload = "{\"badgeId\":\"" + badgeId + "\"}";

        HttpResponse<String> endResponse = sendPost("/end-session", payload);

        assertTrue(endResponse.statusCode() >= 400,
                "End without start should fail, got status=" + endResponse.statusCode());
    }

    private int extractGymCount(String jsonBody) {
        Matcher matcher = GYM_COUNT_PATTERN.matcher(jsonBody);
        assertTrue(matcher.find(), "Expected 'gymCount' in response body: " + jsonBody);
        return Integer.parseInt(matcher.group(1));
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
