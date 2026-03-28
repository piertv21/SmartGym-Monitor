package com.smartgym.embeddedservice.infrastructure.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.DeviceStatusMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class DeviceStatusVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;

    public DeviceStatusVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        vertx.eventBus().<String>consumer(EventBusAddresses.DEVICE_STATUS, msg -> {
            try {
                JsonObject json = new JsonObject(msg.body());
                DeviceStatusMessage deviceStatusMessage = json.mapTo(DeviceStatusMessage.class);

                System.out.println("[DeviceStatusVerticle] Device status event received: " + deviceStatusMessage);

                embeddedService.processDeviceStatus(deviceStatusMessage)
                        .thenAccept(result ->
                                System.out.println("[DeviceStatusVerticle] Device status event processed successfully"))
                        .exceptionally(ex -> {
                            System.err.println("❌ Failed to process device status event: " + ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                System.err.println("❌ Invalid device status event payload: " + e.getMessage());
            }
        });
    }
}