package com.smartgym.embeddedservice.application.ports;

import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface EmbeddedRestController {

    @GetMapping("/statuses")
    CompletableFuture<ResponseEntity<?>> getAllDeviceStatuses();
}
