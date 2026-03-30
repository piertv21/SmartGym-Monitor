package com.smartgym.embeddedservice.infrastructure.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.GymAccessMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class GymAccessVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;

    public GymAccessVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        vertx.eventBus().<String>consumer(EventBusAddresses.GYM_ACCESS, msg -> {
            try {
                JsonObject json = new JsonObject(msg.body());
                GymAccessMessage gymAccessMessage = json.mapTo(GymAccessMessage.class);

                System.out.println("[GymAccessVerticle] Gym access event received: " + gymAccessMessage);

                embeddedService.processGymAccess(gymAccessMessage)
                        .thenAccept(result ->
                                System.out.println("[GymAccessVerticle] Gym access event processed successfully"))
                        .exceptionally(ex -> {
                            System.err.println("❌ Failed to process gym access event: " + ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                System.err.println("❌ Invalid gym access event payload: " + e.getMessage());
            }
        });
    }
}