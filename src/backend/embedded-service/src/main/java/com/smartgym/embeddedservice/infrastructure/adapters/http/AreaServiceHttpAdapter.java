package com.smartgym.embeddedservice.infrastructure.adapters.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.embeddedservice.application.ports.AreaServicePort;
import com.smartgym.embeddedservice.model.AreaAccessMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AreaServiceHttpAdapter implements AreaServicePort {

    private static final Logger logger = LoggerFactory.getLogger(AreaServiceHttpAdapter.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI areaAccessEndpoint;
    private final URI areaExitEndpoint;

    public AreaServiceHttpAdapter(String areaServiceBaseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        String normalizedBaseUrl = areaServiceBaseUrl.endsWith("/")
                ? areaServiceBaseUrl.substring(0, areaServiceBaseUrl.length() - 1)
                : areaServiceBaseUrl;
        this.areaAccessEndpoint = URI.create(normalizedBaseUrl + "/access");
        this.areaExitEndpoint = URI.create(normalizedBaseUrl + "/exit");
        logger.info("🔧 AreaServiceHttpAdapter initialized with base URL: {}", normalizedBaseUrl);
        logger.info("   - Area Access Endpoint: {}", this.areaAccessEndpoint);
        logger.info("   - Area Exit Endpoint: {}", this.areaExitEndpoint);
    }

    @Override
    public CompletableFuture<Void> processAreaAccess(AreaAccessMessage message) {
        final String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid area access payload", ex));
        }

        logger.info("[AreaService] Sending area access request to: {}", areaAccessEndpoint);
        logger.debug("   Request body: {}", requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(areaAccessEndpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.info("[AreaService] area access response received - Status: {}", response.statusCode());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        logger.debug("   Response body: {}", response.body());
                        return response;
                    }
                    throw new IllegalStateException("Area service returned status "
                            + response.statusCode() + ": " + response.body());
                })
                .thenApply(response -> (Void) null)
                .exceptionally(ex -> {
                    logger.error(" [AreaService] area access failed: {}", ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                });
    }

    @Override
    public CompletableFuture<Void> processAreaExit(AreaAccessMessage message) {
        final String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid area exit payload", ex));
        }

        logger.info(" [AreaService] Sending area exit request to: {}", areaExitEndpoint);
        logger.debug("   Request body: {}", requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(areaExitEndpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.info(" [AreaService] area exit response received - Status: {}", response.statusCode());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        logger.debug("   Response body: {}", response.body());
                        return response;
                    }
                    throw new IllegalStateException("Area service returned status "
                            + response.statusCode() + ": " + response.body());
                })
                .thenApply(response -> (Void) null)
                .exceptionally(ex -> {
                    logger.error(" [AreaService] area exit failed: {}", ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                });
    }
}
