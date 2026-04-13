package com.smartgym.embeddedservice.infrastructure.config;

public class MqttConfig {

    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static final String BROKER_HOST = getEnvOrDefault("MQTT_BROKER_HOST", "mosquitto");

    public static final int BROKER_PORT =
            Integer.parseInt(getEnvOrDefault("MQTT_BROKER_PORT", "1883"));

    public static final String BROKER_PROTOCOL = getEnvOrDefault("MQTT_BROKER_PROTOCOL", "tcp");

    public static final String MQTT_USERNAME = getEnvOrDefault("MQTT_USERNAME", "");

    public static final String MQTT_PASSWORD = getEnvOrDefault("MQTT_PASSWORD", "");

    public static final String MQTT_BASE_TOPIC = getEnvOrDefault("MQTT_TOPIC", "smartgym");

    public static final String TOPIC_SUBSCRIBE = MQTT_BASE_TOPIC + "/#";

    public static final String TOPIC_PUBLISH = MQTT_BASE_TOPIC + "/commands";
}
