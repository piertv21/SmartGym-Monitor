package com.smartgym.embeddedservice.application.ports;

import com.smartgym.embeddedservice.model.TicketMessage;
import io.vertx.core.json.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

public interface EmbeddedRestController {

    @PostMapping("/open-entry-barrier")
    CompletableFuture<ResponseEntity<JsonObject>> openEntryBarrier(@RequestBody JsonObject payload);

    @PostMapping("/open-exit-barrier")
    CompletableFuture<ResponseEntity<JsonObject>> openExitBarrier(@RequestBody JsonObject payload);

    @PostMapping("/nfc/enable/{plate}")
    CompletableFuture<ResponseEntity<JsonObject>> setNfcEnable(@PathVariable("plate") String plate, @RequestBody TicketMessage payload);

    @PostMapping("/nfc/disable")
    CompletableFuture<ResponseEntity<JsonObject>> setNfcDisabled(@RequestBody JsonObject payload);

    @PostMapping("/payment-confirmed")
    CompletableFuture<ResponseEntity<JsonObject>> confirmPayment(@RequestBody JsonObject payload);

    @GetMapping("/status")
    CompletableFuture<ResponseEntity<Object>> getEmbeddedDeviceStatus();

    @GetMapping("/getPendingPayment")
    CompletableFuture<ResponseEntity<JsonObject>> getPending();

    @GetMapping("/deletePendingPayment")
    CompletableFuture<ResponseEntity<JsonObject>> deletePending();
}

