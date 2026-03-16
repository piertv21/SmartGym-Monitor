package com.smartgym.embeddedservice.infrastracture.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.HelpButtonMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class HelpButtonVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;

    public HelpButtonVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        vertx.eventBus().<HelpButtonMessage>consumer("event.helpbutton", msg -> {
            JsonObject json = JsonObject.mapFrom(msg.body());
            HelpButtonMessage help = json.mapTo(HelpButtonMessage.class);
            System.out.println("[HelpButtonVerticle] Help detection event: " + help);
            embeddedService.registerEvent("helpbutton", json)
                    .exceptionally(ex -> {
                        System.err.println("❌ Failed to save coil event: " + ex.getMessage());
                        return null;
                    });
        });
    }
}
