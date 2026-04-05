package com.smartgym.embeddedservice.application;

import com.smartgym.embeddedservice.application.ports.EmbeddedRestController;
import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
public class EmbeddedRestControllerImpl implements EmbeddedRestController {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedRestControllerImpl.class);
    private final EmbeddedServiceAPI embeddedService;

    public EmbeddedRestControllerImpl(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    @GetMapping("/statuses")
    public CompletableFuture<ResponseEntity<?>> getAllDeviceStatuses() {
        return embeddedService.getAllDeviceStatuses()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Failed to retrieve device statuses", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new JsonObject().put("error", "Unable to retrieve device statuses"));
                });
    }

}
