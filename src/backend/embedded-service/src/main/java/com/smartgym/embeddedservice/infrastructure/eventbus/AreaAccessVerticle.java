package com.smartgym.embeddedservice.infrastructure.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.AreaAccessMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class AreaAccessVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;

    public AreaAccessVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        vertx.eventBus().<String>consumer(EventBusAddresses.AREA_ACCESS, msg -> {
            try {
                JsonObject json = new JsonObject(msg.body());
                AreaAccessMessage areaAccessMessage = json.mapTo(AreaAccessMessage.class);

                System.out.println("[AreaAccessVerticle] Area access event received: " + areaAccessMessage);

                embeddedService.processAreaAccess(areaAccessMessage)
                        .thenAccept(result ->
                                System.out.println("[AreaAccessVerticle] Area access event processed successfully"))
                        .exceptionally(ex -> {
                            System.err.println("❌ Failed to process area access event: " + ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                System.err.println("❌ Invalid area access event payload: " + e.getMessage());
            }
        });
    }
}