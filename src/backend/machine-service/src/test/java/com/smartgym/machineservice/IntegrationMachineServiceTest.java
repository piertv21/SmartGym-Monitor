package com.smartgym.machineservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationMachineServiceTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final String baseUrl =
            System.getenv().getOrDefault("MACHINE_SERVICE_IT_BASE_URL", "http://localhost:8084");

    @BeforeAll
    void checkPreconditions() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("SMARTGYM_IT_ENABLED", "true")),
                "Set SMARTGYM_IT_ENABLED=true and start docker compose before running integration tests");
        Assumptions.assumeTrue(
                isServiceHealthy(), "Machine service is not reachable on " + baseUrl);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        HttpResponse<String> response = sendGet("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void createMachineAndReadStatus() throws Exception {
        String machineId = "machine-" + System.currentTimeMillis();
        String createBody =
                """
                {
                  "machineId": "%s",
                  "areaId": "machines-area",
                  "sensor": "sensor-%s"
                }
                """
                        .formatted(machineId, machineId);

        HttpResponse<String> createResponse = sendPost("/machines", createBody);
        assertEquals(201, createResponse.statusCode());

        HttpResponse<String> statusResponse = sendGet("/" + machineId);
        assertEquals(200, statusResponse.statusCode());
        assertTrue(statusResponse.body().contains(machineId));
    }

    @Test
    void getAllMachinesReturnsArray() throws Exception {
        HttpResponse<String> response = sendGet("/machines");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
    }

    @Test
    void startAndEndMachineSession() throws Exception {
        String machineId = "treadmill-01"; // seeded machine

        // Ensure machine is free first
        HttpResponse<String> status = sendGet("/" + machineId);
        assertEquals(200, status.statusCode());
        if (status.body().contains("OCCUPIED")) {
            sendPost("/end-session", "{\"machineId\":\"" + machineId + "\"}");
        }

        String badgeId = "it-badge-" + UUID.randomUUID();
        String startPayload =
                """
                { "machineId": "%s", "badgeId": "%s" }
                """
                        .formatted(machineId, badgeId);

        HttpResponse<String> startResponse = sendPost("/start-session", startPayload);
        assertEquals(200, startResponse.statusCode());
        assertTrue(startResponse.body().contains("\"sessionId\""));

        // Verify machine is occupied
        HttpResponse<String> occupiedStatus = sendGet("/" + machineId);
        assertEquals(200, occupiedStatus.statusCode());
        assertTrue(occupiedStatus.body().contains("OCCUPIED"));

        // End session
        String endPayload =
                """
                { "machineId": "%s" }
                """
                        .formatted(machineId);

        HttpResponse<String> endResponse = sendPost("/end-session", endPayload);
        assertEquals(200, endResponse.statusCode());

        // Verify machine is free again
        HttpResponse<String> freeStatus = sendGet("/" + machineId);
        assertEquals(200, freeStatus.statusCode());
        assertTrue(freeStatus.body().contains("FREE"));
    }

    @Test
    void startSessionOnOccupiedMachineFails() throws Exception {
        String machineId = "machine-conflict-" + System.currentTimeMillis();
        String createBody =
                """
                { "machineId": "%s", "areaId": "machines-area", "sensor": "sensor-%s" }
                """
                        .formatted(machineId, machineId);
        sendPost("/machines", createBody);

        String startPayload =
                """
                { "machineId": "%s", "badgeId": "badge-first" }
                """
                        .formatted(machineId);
        HttpResponse<String> firstStart = sendPost("/start-session", startPayload);
        assertEquals(200, firstStart.statusCode());

        // Second start on same machine should fail
        String secondStartPayload =
                """
                { "machineId": "%s", "badgeId": "badge-second" }
                """
                        .formatted(machineId);
        HttpResponse<String> secondStart = sendPost("/start-session", secondStartPayload);
        assertTrue(
                secondStart.statusCode() >= 400,
                "Start on occupied machine should fail, got status=" + secondStart.statusCode());

        // Cleanup
        sendPost("/end-session", "{\"machineId\":\"" + machineId + "\"}");
    }

    @Test
    void getMachineHistoryReturnsOk() throws Exception {
        HttpResponse<String> response = sendGet("/history/treadmill-01");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
    }

    @Test
    void getMachineUsageSeriesReturnsOk() throws Exception {
        HttpResponse<String> response =
                sendGet("/machines/history/series?from=2026-04-01&to=2026-04-12&granularity=daily");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"granularity\""));
        assertTrue(response.body().contains("\"series\""));
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

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(10))
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
