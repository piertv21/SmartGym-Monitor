package com.smartgym.analyticsservice;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationAnalyticsServiceTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final String baseUrl = System.getenv().getOrDefault("ANALYTICS_SERVICE_IT_BASE_URL", "http://localhost:8085");

    @BeforeAll
    void checkPreconditions() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("SMARTGYM_IT_ENABLED", "true")),
                "Set SMARTGYM_IT_ENABLED=true and start docker compose before running integration tests"
        );
        Assumptions.assumeTrue(isServiceHealthy(), "Analytics service is not reachable on " + baseUrl);
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        HttpResponse<String> response = sendGet("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void ingestEventAndReadAttendanceByDate() throws Exception {
        String date = "2026-04-01";
        String payload = """
                {
                  "eventType": "GYM_ACCESS",
                  "payload": {
                    "timeStamp": "2026-04-01T09:00:00Z",
                    "accessType": "ENTRY"
                  }
                }
                """;

        HttpResponse<String> ingest = sendPost("/events/ingest", payload);
        assertEquals(202, ingest.statusCode());

        HttpResponse<String> attendance = sendGet("/attendance/" + date);
        assertEquals(200, attendance.statusCode());
        assertTrue(attendance.body().contains(date));
    }

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

