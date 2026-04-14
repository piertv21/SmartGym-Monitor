package com.smartgym.embeddedservice.infrastructure.adapters.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.embeddedservice.application.ports.MachineServicePort;
import com.smartgym.embeddedservice.model.MachineUsageMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MachineServiceHttpAdapter implements MachineServicePort {

    private static final Logger logger = LoggerFactory.getLogger(MachineServiceHttpAdapter.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI startSessionEndpoint;
    private final URI endSessionEndpoint;

    public MachineServiceHttpAdapter(String machineServiceBaseUrl) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
        this.objectMapper = new ObjectMapper();
        String normalizedBaseUrl =
                machineServiceBaseUrl.endsWith("/")
                        ? machineServiceBaseUrl.substring(0, machineServiceBaseUrl.length() - 1)
                        : machineServiceBaseUrl;
        this.startSessionEndpoint = URI.create(normalizedBaseUrl + "/start-session");
        this.endSessionEndpoint = URI.create(normalizedBaseUrl + "/end-session");
        logger.info(
                "🔧 MachineServiceHttpAdapter initialized with base URL: {}", normalizedBaseUrl);
        logger.info("   - Start Session Endpoint: {}", this.startSessionEndpoint);
        logger.info("   - End Session Endpoint: {}", this.endSessionEndpoint);
    }

    @Override
    public CompletableFuture<Void> startSession(MachineUsageMessage message) {
        logger.debug(
                "📤 Preparing to call start-session for machine: {}, badge: {}",
                message.getMachineId(),
                message.getBadgeId());
        return post(startSessionEndpoint, toStartSessionBody(message), "startSession");
    }

    @Override
    public CompletableFuture<Void> endSession(MachineUsageMessage message) {
        logger.debug("Preparing to call end-session for machine: {}", message.getMachineId());
        return post(endSessionEndpoint, toEndSessionBody(message), "endSession");
    }

    private String toStartSessionBody(MachineUsageMessage message) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of(
                            "machineId", message.getMachineId(),
                            "badgeId", message.getBadgeId()));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid machine start-session payload", ex);
        }
    }

    private String toEndSessionBody(MachineUsageMessage message) {
        try {
            return objectMapper.writeValueAsString(Map.of("machineId", message.getMachineId()));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid machine end-session payload", ex);
        }
    }

    private CompletableFuture<Void> post(URI endpoint, String requestBody, String operationName) {
        try {
            logger.info("[MachineService] Sending {} request to: {}", operationName, endpoint);
            logger.debug("   Request body: {}", requestBody);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(endpoint)
                            .timeout(REQUEST_TIMEOUT)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(
                            response -> {
                                logger.info(
                                        "[MachineService] {} response received - Status: {}",
                                        operationName,
                                        response.statusCode());
                                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                    logger.debug("   Response body: {}", response.body());
                                    return response;
                                }
                                throw new IllegalStateException(
                                        "Machine service returned status "
                                                + response.statusCode()
                                                + ": "
                                                + response.body());
                            })
                    .thenApply(response -> (Void) null)
                    .exceptionally(
                            ex -> {
                                logger.error(
                                        "[MachineService] {} failed with error: {}",
                                        operationName,
                                        ex.getMessage(),
                                        ex);
                                throw new RuntimeException(ex);
                            });
        } catch (Exception ex) {
            logger.error(
                    "[MachineService] {} preparation failed: {}",
                    operationName,
                    ex.getMessage(),
                    ex);
            return CompletableFuture.failedFuture(ex);
        }
    }
}
