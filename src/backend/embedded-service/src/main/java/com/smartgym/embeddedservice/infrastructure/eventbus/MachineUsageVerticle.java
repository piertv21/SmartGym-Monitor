package com.smartgym.embeddedservice.infrastructure.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.MachineUsageMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class MachineUsageVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;

    public MachineUsageVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        vertx.eventBus().<String>consumer(EventBusAddresses.MACHINE_USAGE, msg -> {
            try {
                JsonObject json = new JsonObject(msg.body());
                MachineUsageMessage machineUsageMessage = json.mapTo(MachineUsageMessage.class);

                System.out.println("[MachineUsageVerticle] Machine usage event received: " + machineUsageMessage);

                embeddedService.processMachineUsage(machineUsageMessage)
                        .thenAccept(result ->
                                System.out.println("[MachineUsageVerticle] Machine usage event processed successfully"))
                        .exceptionally(ex -> {
                            System.err.println("❌ Failed to process machine usage event: " + ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                System.err.println("❌ Invalid machine usage event payload: " + e.getMessage());
            }
        });
    }
}