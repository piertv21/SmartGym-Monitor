package com.smartgym.embeddedservice.application.ports;

import io.vertx.core.json.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

public interface EmbeddedRestController {

    @GetMapping("/statuses")
    CompletableFuture<ResponseEntity<?>> getAllDeviceStatuses();

}

