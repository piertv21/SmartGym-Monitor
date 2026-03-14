package com.smartgym.embeddedservice.infrastracture.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.CoilDetectionMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class CoilDetectionVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;

    public CoilDetectionVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        vertx.eventBus().<CoilDetectionMessage>consumer("event.coil", msg -> {
            JsonObject json = JsonObject.mapFrom(msg.body());
            CoilDetectionMessage coil = json.mapTo(CoilDetectionMessage.class);
            System.out.println("[CoilDetectionVerticle] Coil detection event: " + coil);

            embeddedService.registerEvent("coil", json)
            .exceptionally(ex -> {
                System.err.println("❌ Failed to save coil event: " + ex.getMessage());
                return null;
            });
        });
    }
}
