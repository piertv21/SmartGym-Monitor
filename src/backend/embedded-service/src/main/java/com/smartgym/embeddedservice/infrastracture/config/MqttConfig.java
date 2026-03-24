package com.smartgym.embeddedservice.infrastracture.config;

public class MqttConfig {
    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static final String BROKER_HOST = getEnvOrDefault("MQTT_BROKER_HOST", "mosquitto");
    public static final int BROKER_PORT = Integer.parseInt(getEnvOrDefault("MQTT_BROKER_PORT", "1883"));
    public static final String BROKER_PROTOCOL = getEnvOrDefault("MQTT_BROKER_PROTOCOL", "tcp");
    public static final String MQTT_USERNAME = getEnvOrDefault("MQTT_USERNAME", "SmartGym-Monitor");
    public static final String MQTT_PASSWORD = getEnvOrDefault("MQTT_PASSWORD", "SmartGym-Monitor1");

    public static final String TOPIC_SUBSCRIBE = "parking/sensor/#";
    public static final String TOPIC_PUBLISH = "parking/commands";
}
