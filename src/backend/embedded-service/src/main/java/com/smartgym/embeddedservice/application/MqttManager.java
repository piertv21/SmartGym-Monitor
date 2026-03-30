package com.smartgym.embeddedservice.application;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.infrastructure.adapters.mqtt.VertxMqttClientAdapter;
import com.smartgym.embeddedservice.infrastructure.eventbus.AreaAccessVerticle;
import com.smartgym.embeddedservice.infrastructure.eventbus.DeviceStatusVerticle;
import com.smartgym.embeddedservice.infrastructure.eventbus.GymAccessVerticle;
import com.smartgym.embeddedservice.infrastructure.eventbus.MachineUsageVerticle;
import io.vertx.core.Vertx;

public class MqttManager {

    private final Vertx vertx;
    private final EmbeddedServiceAPI embeddedService;

    public MqttManager(Vertx vertx, EmbeddedServiceAPI embeddedService) {
        this.vertx = vertx;
        this.embeddedService = embeddedService;
    }

    public void start() {
        vertx.deployVerticle(new VertxMqttClientAdapter());
        vertx.deployVerticle(new GymAccessVerticle(embeddedService));
        vertx.deployVerticle(new AreaAccessVerticle(embeddedService));
        vertx.deployVerticle(new MachineUsageVerticle(embeddedService));
        vertx.deployVerticle(new DeviceStatusVerticle(embeddedService));

        System.out.println("✅ All SmartGym Verticles Deployed Successfully");
    }

    public void publish(String topic, String message) {
        System.out.println("[MqttManager] Publishing to topic " + topic + "...");
        VertxMqttClientAdapter.publish(topic, message);
    }
}