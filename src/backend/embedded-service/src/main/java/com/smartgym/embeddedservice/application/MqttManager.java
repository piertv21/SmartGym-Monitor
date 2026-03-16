package com.smartgym.embeddedservice.application;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.infrastracture.eventbus.*;
import io.vertx.core.Vertx;
import com.smartgym.embeddedservice.infrastracture.adapters.mqtt.VertxMqttClientAdapter;

public class MqttManager {

    private final Vertx vertx;
    private final EmbeddedServiceAPI embeddedService;

    public MqttManager(Vertx vertx, EmbeddedServiceAPI embeddedService) {
        this.vertx = vertx;
        this.embeddedService = embeddedService;
    }

    public void start() {
        vertx.deployVerticle(new VertxMqttClientAdapter());
        vertx.deployVerticle(new HelpButtonVerticle(embeddedService));
        vertx.deployVerticle(new CoilDetectionVerticle(embeddedService));
        vertx.deployVerticle(new PlateDetectionVerticle(embeddedService));
        vertx.deployVerticle(new NfcVerticle(embeddedService));
        vertx.deployVerticle(new KeyboardInsertionVerticle(embeddedService));

        System.out.println("✅ All Verticles Deployed Successfully");
    }

    public void publish(String topic, String message) {
        System.out.println(" [MqttManager] Publishing to topic " + topic + "...");
        VertxMqttClientAdapter.publish(topic, message);
    }

    public void subscribe(String topic, String message) {
        System.out.println(" [MqttManager] Publishing to topic " + topic + "...");
        VertxMqttClientAdapter.publish(topic, message);
    }
}

