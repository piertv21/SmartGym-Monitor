package com.smartgym.analyticsservice.infrastructure.adapters;

import com.smartgym.analyticsservice.application.ports.DummyServicePort;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.concurrent.CompletableFuture;

/**
 * Adapter Spring WebClient verso dummy-service
 */
public class DummyServiceAdapter implements DummyServicePort {

    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(DummyServiceAdapter.class);

    public DummyServiceAdapter() {
        this.webClient = WebClient.builder().build();
    }


}
