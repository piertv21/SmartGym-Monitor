package com.smartgym.embeddedservice.infrastracture.config;

public class MqttConfig {
    public static final String BROKER_HOST = "4629b95c886c4677911c2126077efb80.s2.eu.hivemq.cloud";
    public static final int BROKER_PORT = 8883;
    public static final String MQTT_USERNAME = "ESP32-CAM";
    public static final String MQTT_PASSWORD = "Davide00";

    public static final String TOPIC_SUBSCRIBE = "parking/sensor/#";
    public static final String TOPIC_PUBLISH = "parking/commands";
}
