package com.smartgym.embeddedservice.application;

import com.smartgym.embeddedservice.application.ports.EmbeddedRestController;
import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.infrastracture.adapters.mqtt.VertxMqttClientAdapter;
import com.smartgym.embeddedservice.model.TicketMessage;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class EmbeddedRestControllerImpl implements EmbeddedRestController {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedRestControllerImpl.class);
    private final MqttManager mqttManager;
    private final EmbeddedServiceAPI embeddedService;

    public EmbeddedRestControllerImpl(MqttManager mqttManager, EmbeddedServiceAPI embeddedService) {
        this.mqttManager = mqttManager;
        this.embeddedService = embeddedService;
    }

    @PostMapping("/open-entry-barrier")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> openEntryBarrier(
            @RequestBody JsonObject payload) {
        return sendCommand("openEntry", "ENTRY BARRIER OPEN COMMAND SENT");
    }

    @PostMapping("/open-exit-barrier")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> openExitBarrier(@RequestBody JsonObject payload) {
        return sendCommand("openExit", "EXIT BARRIER OPEN COMMAND SENT");
    }

    @PostMapping("/nfc/enable/{plate}")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> setNfcEnable(@PathVariable String plate, @RequestBody TicketMessage payload) { //IL PROBLEMA E QUIII DEVO CREARE UNA CLASSE JACKSON PER CONVERTIRE IL PAYLOAD

        System.out.println(">>> setNfcEnable CALLED WITH: " + payload.toString());

        if (plate == null || payload == null) {
            JsonObject error = new JsonObject().put("error", "Missing plate");
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(error)
            );
        }

        CompletableFuture<TicketMessage> saveFuture = embeddedService.saveNfcPendingPayment(payload);

        return saveFuture.thenApply(v -> {
            sendCommand("setNfcEnable", "NFC PENDING PAYMENT");
            return ResponseEntity.ok(new JsonObject().put("status", "OK"));
        });
    }

    @PostMapping("/nfc/disable")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> setNfcDisabled(
            @RequestBody JsonObject payload) {
        return sendCommand("nfcDisable", "NFC DISABLE SENT");
    }

    @PostMapping("/payment-confirmed")
    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> confirmPayment(
            @RequestBody JsonObject payload) {
        return sendCommand("paymentConfirmed", "PAYMENT CONFIRMATION SENT");
    }

    @GetMapping("/status")
    @Override
    public CompletableFuture<ResponseEntity<Object>> getEmbeddedDeviceStatus() {

        return CompletableFuture.supplyAsync(() -> {
            Map<String, JsonObject> allStatuses = VertxMqttClientAdapter.getAllStatuses();
            io.vertx.core.json.JsonArray sensors = new io.vertx.core.json.JsonArray();

            allStatuses.forEach((deviceId, statusJson) -> {
                JsonObject sensor = new JsonObject()
                        .put("deviceId", statusJson.getString("deviceId"))
                        .put("status",   statusJson.getString("status"));

                sensors.add(sensor);
            });

            JsonObject response = new JsonObject().put("sensors", sensors);
            return ResponseEntity.ok(response);
        });
    }

    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> getPending() {

        return embeddedService.getPendingPayment()
                .thenApply(json -> ResponseEntity.ok(json))
                .exceptionally(ex -> {
                    JsonObject error = new JsonObject()
                            .put("error", "Failed to get pending: " + ex.getMessage());
                    return ResponseEntity.internalServerError().body(error);
                });
    }

    @Override
    public CompletableFuture<ResponseEntity<JsonObject>> deletePending() {

        return embeddedService.deletePendingPayment()
                .thenApply(result -> {

                    JsonObject resp = new JsonObject()
                            .put("deleted", result.getBoolean("deleted"))
                            .put("pending", result.getBoolean("pending"));

                    if (result.containsKey("removed_id")) {
                        resp.put("removed_id", result.getString("removed_id"));
                    }

                    return ResponseEntity.ok(resp);
                })
                .exceptionally(ex -> {
                    JsonObject error = new JsonObject()
                            .put("error", "Failed to delete pending payment: " + ex.getMessage());

                    return ResponseEntity
                            .internalServerError()
                            .body(error);
                });
    }

    private CompletableFuture<ResponseEntity<JsonObject>> sendCommand(
            String command, String messageStatus) {
        try {
            JsonObject message = new JsonObject().put("command", command);
            logger.info("📤 Sending command '{}' to topic parking/sensor/cmd", command);

            mqttManager.publish("parking/sensor/cmd", message.encode());

            JsonObject response = new JsonObject()
                    .put("status", messageStatus)
                    .put("command", command)
                    .put("topic", "parking/sensor/cmd");

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));

        } catch (Exception e) {

            logger.error("❌ Error sending command {}: {}", command, e.getMessage());

            JsonObject error = new JsonObject()
                    .put("error", "Failed to publish command")
                    .put("command", command)
                    .put("details", e.getMessage());

            return CompletableFuture.completedFuture(
                    ResponseEntity.internalServerError().body(error)
            );
        }
    }
}
