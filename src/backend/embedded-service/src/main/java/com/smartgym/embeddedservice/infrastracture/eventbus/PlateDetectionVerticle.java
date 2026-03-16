package com.smartgym.embeddedservice.infrastracture.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.PlateDetectionMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.Objects;

public class PlateDetectionVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;
    private WebClient webClient;
    private static final String PARKING_SERVICE_URL = "http://parking-service:8082";

    public PlateDetectionVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        webClient = WebClient.create(vertx);

        vertx.eventBus().<PlateDetectionMessage>consumer("event.camera", msg -> {
            JsonObject json = JsonObject.mapFrom(msg.body());
            PlateDetectionMessage plateMsg = json.mapTo(PlateDetectionMessage.class);
            System.out.println("[PlateDetectionVerticle] Plate detection event: " + plateMsg);
            embeddedService.registerEvent("plate", json)
                    .exceptionally(ex -> {
                        System.err.println("❌ Failed to save plate event: " + ex.getMessage());
                        return null;
                    });
            if (Objects.equals(plateMsg.getCameraAt(), "cameraIn")) {
                JsonObject requestBody = new JsonObject()
                        .put("plate", plateMsg.getLicensePlate());
                webClient.postAbs(PARKING_SERVICE_URL + "/addCar")
                        .putHeader("Content-Type", "application/json")
                        .as(BodyCodec.jsonObject())
                        .sendJsonObject(requestBody, ar -> {
                            if (ar.succeeded()) {
                                JsonObject response = ar.result().body();
                                System.out.println("🚗 Request to Parking-Service OK: " + response.encodePrettily());

                            } else {
                                System.err.println("❌ Failed to contact Parking-Service: " + ar.cause().getMessage());
                            }
                        });
            }
            else if (Objects.equals(plateMsg.getCameraAt(), "cameraOut")) {

                JsonObject requestBody = new JsonObject()
                        .put("plate", plateMsg.getLicensePlate());
                webClient.postAbs(PARKING_SERVICE_URL + "/removeCar")
                        .putHeader("Content-Type", "application/json")
                        .as(BodyCodec.jsonObject())
                        .sendJsonObject(requestBody, ar -> {
                            if (ar.succeeded()) {
                                JsonObject response = ar.result().body();
                                System.out.println("🚗 Request to Parking-Service TO REMOVE CAR OK: " + response.encodePrettily());
                            } else {
                                System.err.println("❌ Failed to contact Parking-Service: " + ar.cause().getMessage());
                            }
                        });

            }

        });
    }
}
