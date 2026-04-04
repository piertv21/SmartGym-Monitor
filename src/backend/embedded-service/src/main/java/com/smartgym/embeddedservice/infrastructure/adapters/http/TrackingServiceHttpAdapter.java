package com.smartgym.embeddedservice.infrastructure.adapters.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.embeddedservice.application.ports.TrackingServicePort;
import com.smartgym.embeddedservice.model.GymAccessMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TrackingServiceHttpAdapter implements TrackingServicePort {

    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceHttpAdapter.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI startSessionEndpoint;
    private final URI endSessionEndpoint;

    public TrackingServiceHttpAdapter(String trackingServiceBaseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();

        String normalizedBaseUrl = trackingServiceBaseUrl.endsWith("/")
                ? trackingServiceBaseUrl.substring(0, trackingServiceBaseUrl.length() - 1)
                : trackingServiceBaseUrl;

        this.startSessionEndpoint = URI.create(normalizedBaseUrl + "/start-session");
        this.endSessionEndpoint = URI.create(normalizedBaseUrl + "/end-session");

        logger.info("TrackingServiceHttpAdapter initialized with base URL: {}", normalizedBaseUrl);
        logger.info(" - Start Session Endpoint: {}", this.startSessionEndpoint);
        logger.info(" - End Session Endpoint: {}", this.endSessionEndpoint);
    }

    @Override
    public CompletableFuture<Void> startGymSession(GymAccessMessage message) {
        return post(startSessionEndpoint, toBadgeOnlyBody(message), "startGymSession");
    }

    @Override
    public CompletableFuture<Void> endGymSession(GymAccessMessage message) {
        return post(endSessionEndpoint, toBadgeOnlyBody(message), "endGymSession");
    }

    private String toBadgeOnlyBody(GymAccessMessage message) {
        if (message == null || isBlank(message.getBadgeId())) {
            throw new IllegalArgumentException("badgeId is required");
        }
        try {
            return objectMapper.writeValueAsString(Map.of("badgeId", message.getBadgeId()));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid tracking payload", ex);
        }
    }

    private CompletableFuture<Void> post(URI endpoint, String requestBody, String operationName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response;
                    }
                    throw new IllegalStateException("Tracking service returned status "
                            + response.statusCode() + ": " + response.body());
                })
                .thenApply(response -> (Void) null)
                .exceptionally(ex -> {
                    logger.error("{} failed: {}", operationName, ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}