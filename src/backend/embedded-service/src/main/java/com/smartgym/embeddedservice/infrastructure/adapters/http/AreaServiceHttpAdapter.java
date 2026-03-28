package com.smartgym.embeddedservice.infrastructure.adapters.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.embeddedservice.application.ports.AreaServicePort;
import com.smartgym.embeddedservice.model.AreaAccessMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AreaServiceHttpAdapter implements AreaServicePort {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI areaAccessEndpoint;

    public AreaServiceHttpAdapter(String areaServiceBaseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        String normalizedBaseUrl = areaServiceBaseUrl.endsWith("/")
                ? areaServiceBaseUrl.substring(0, areaServiceBaseUrl.length() - 1)
                : areaServiceBaseUrl;
        this.areaAccessEndpoint = URI.create(normalizedBaseUrl + "/area-service/access");
    }

    @Override
    public CompletableFuture<Void> processAreaAccess(AreaAccessMessage message) {
        final String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid area access payload", ex));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(areaAccessEndpoint)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.failedFuture(
                            new IllegalStateException("Area service returned status "
                                    + response.statusCode() + ": " + response.body()));
                });
    }
}

