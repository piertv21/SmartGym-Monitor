package com.smartgym.embeddedservice.infrastructure.adapters.http;

import com.smartgym.embeddedservice.application.ports.AnalyticsServicePort;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyticsServiceHttpAdapter implements AnalyticsServicePort {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceHttpAdapter.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final URI ingestEndpoint;

    public AnalyticsServiceHttpAdapter(String analyticsServiceBaseUrl) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
        String normalizedBaseUrl =
                analyticsServiceBaseUrl.endsWith("/")
                        ? analyticsServiceBaseUrl.substring(0, analyticsServiceBaseUrl.length() - 1)
                        : analyticsServiceBaseUrl;
        this.ingestEndpoint = URI.create(normalizedBaseUrl + "/events/ingest");
        logger.info(
                "🔧 AnalyticsServiceHttpAdapter initialized with base URL: {}", normalizedBaseUrl);
        logger.info("   - Ingest Endpoint: {}", this.ingestEndpoint);
    }

    @Override
    public CompletableFuture<Void> ingestEvent(JsonObject event) {
        if (event == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("event cannot be null"));
        }

        final String requestBody = event.encode();

        logger.info("🌐 [AnalyticsService] Sending ingest event request to: {}", ingestEndpoint);
        logger.debug("   Request body: {}", requestBody);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(ingestEndpoint)
                        .timeout(REQUEST_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(
                        response -> {
                            logger.info(
                                    "✅ [AnalyticsService] ingest event response received - Status: {}",
                                    response.statusCode());
                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                logger.debug("   Response body: {}", response.body());
                                return response;
                            }
                            throw new IllegalStateException(
                                    "Analytics service returned status "
                                            + response.statusCode()
                                            + ": "
                                            + response.body());
                        })
                .thenApply(response -> (Void) null)
                .exceptionally(
                        ex -> {
                            logger.error(
                                    "❌ [AnalyticsService] ingest event failed: {}",
                                    ex.getMessage(),
                                    ex);
                            throw new RuntimeException(ex);
                        });
    }
}
