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
import java.time.LocalDate;
import java.util.UUID;

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
    void ingestEventAndReadAttendance() throws Exception {
        String date = LocalDate.now().plusYears(5).toString();
        String badgeId = "it-badge-" + UUID.randomUUID();

        String gymEntryPayload = """
                {
                  "eventType": "GYM_ACCESS",
                  "payload": {
                    "timeStamp": "%sT09:00:00Z",
                    "accessType": "ENTRY",
                    "badgeId": "%s"
                  }
                }
                """.formatted(date, badgeId);

        String gymExitPayload = """
                {
                  "eventType": "GYM_ACCESS",
                  "payload": {
                    "timeStamp": "%sT09:25:00Z",
                    "accessType": "EXIT",
                    "badgeId": "%s"
                  }
                }
                """.formatted(date, badgeId);

        assertEquals(202, sendPost("/events/ingest", gymEntryPayload).statusCode());
        assertEquals(202, sendPost("/events/ingest", gymExitPayload).statusCode());

        HttpResponse<String> attendance = sendGet("/attendance");
        assertEquals(200, attendance.statusCode());

        HttpResponse<String> gymDuration = sendGet("/gym-session-duration/" + date);
        assertEquals(200, gymDuration.statusCode());
        assertTrue(gymDuration.body().contains("\"sessionCount\":"));

        HttpResponse<String> series = sendGet("/attendance/series?from=" + date + "&to=" + date + "&granularity=daily");
        assertEquals(200, series.statusCode());
        assertTrue(series.body().contains("\"granularity\":\"daily\""));
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
