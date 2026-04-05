package com.smartgym.embeddedservice.infrastructure.adapters.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.embeddedservice.infrastructure.config.MqttConfig;
import com.smartgym.embeddedservice.infrastructure.eventbus.EventBusAddresses;
import com.smartgym.embeddedservice.model.AreaAccessMessage;
import com.smartgym.embeddedservice.model.DeviceStatusMessage;
import com.smartgym.embeddedservice.model.GymAccessMessage;
import com.smartgym.embeddedservice.model.MachineUsageMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VertxMqttClientAdapter extends AbstractVerticle {

    private static MqttClient clientInstance;
    private static final Map<String, JsonObject> sensorStatusMap = new ConcurrentHashMap<>();

    @Override
    public void start() {
        final boolean useSsl = "tls".equalsIgnoreCase(MqttConfig.BROKER_PROTOCOL)
                || "ssl".equalsIgnoreCase(MqttConfig.BROKER_PROTOCOL);

        final MqttClientOptions options = new MqttClientOptions()
                .setSsl(useSsl)
                .setTrustAll(useSsl);

        if (!MqttConfig.MQTT_USERNAME.isBlank()) {
            options.setUsername(MqttConfig.MQTT_USERNAME);
        }

        if (!MqttConfig.MQTT_PASSWORD.isBlank()) {
            options.setPassword(MqttConfig.MQTT_PASSWORD);
        }

        final MqttClient client = MqttClient.create(vertx, options);
        final ObjectMapper mapper = new ObjectMapper();
        final String baseTopic = MqttConfig.MQTT_BASE_TOPIC;

        client.connect(MqttConfig.BROKER_PORT, MqttConfig.BROKER_HOST, result -> {
            if (result.succeeded()) {
                System.out.println("Connected to MQTT broker");
                clientInstance = client;

                client.subscribe(MqttConfig.TOPIC_SUBSCRIBE, 1, subResult -> {
                    if (subResult.succeeded()) {
                        System.out.println("Subscribed to topic: " + MqttConfig.TOPIC_SUBSCRIBE);
                    } else {
                        System.err.println("MQTT subscription failed: " + subResult.cause().getMessage());
                    }
                });
            } else {
                System.err.println("MQTT connection failed: " + result.cause().getMessage());
            }
        });

        client.publishHandler(msg -> {
            final String topic = msg.topicName();
            final String payload = msg.payload().toString(StandardCharsets.UTF_8);

            System.out.println("--- Received message on topic: " + topic);

            try {
                if (topic.equals(baseTopic + "/gym-access")) {
                    GymAccessMessage gymAccessMessage = mapper.readValue(payload, GymAccessMessage.class);
                    vertx.eventBus().publish(
                            EventBusAddresses.GYM_ACCESS,
                            JsonObject.mapFrom(gymAccessMessage).encode()
                    );
                    return;
                }

                if (topic.equals(baseTopic + "/area-access")) {
                    AreaAccessMessage areaAccessMessage = mapper.readValue(payload, AreaAccessMessage.class);
                    vertx.eventBus().publish(
                            EventBusAddresses.AREA_ACCESS,
                            JsonObject.mapFrom(areaAccessMessage).encode()
                    );
                    return;
                }

                if (topic.equals(baseTopic + "/machine-usage")) {
                    MachineUsageMessage machineUsageMessage = mapper.readValue(payload, MachineUsageMessage.class);
                    vertx.eventBus().publish(
                            EventBusAddresses.MACHINE_USAGE,
                            JsonObject.mapFrom(machineUsageMessage).encode()
                    );
                    return;
                }

                if (topic.equals(baseTopic + "/device-status")) {
                    DeviceStatusMessage deviceStatusMessage = mapper.readValue(payload, DeviceStatusMessage.class);
                    JsonObject json = JsonObject.mapFrom(deviceStatusMessage);

                    if (deviceStatusMessage.getDeviceId() != null && !deviceStatusMessage.getDeviceId().isBlank()) {
                        sensorStatusMap.put(deviceStatusMessage.getDeviceId(), json);
                    }

                    vertx.eventBus().publish(EventBusAddresses.DEVICE_STATUS, json.encode());
                    return;
                }

                if (topic.startsWith(baseTopic + "/") && topic.endsWith("/status")) {
                    DeviceStatusMessage statusMessage = mapper.readValue(payload, DeviceStatusMessage.class);
                    JsonObject json = JsonObject.mapFrom(statusMessage);

                    if (statusMessage.getDeviceId() != null && !statusMessage.getDeviceId().isBlank()) {
                        sensorStatusMap.put(statusMessage.getDeviceId(), json);
                    }

                    System.out.println("Updated status for device [" +
                            statusMessage.getDeviceId() + "]: " + json.encodePrettily());
                    vertx.eventBus().publish(EventBusAddresses.DEVICE_STATUS, json.encode());
                    return;
                }

                System.out.println("⚠ Unknown topic received: " + topic);

            } catch (Exception e) {
                System.err.println("Error parsing MQTT message on topic [" + topic + "]: " + e.getMessage());
            }
        });
    }

    public static void publish(String topic, String message) {
        if (clientInstance != null && clientInstance.isConnected()) {
            clientInstance.publish(
                    topic,
                    Buffer.buffer(message),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false
            );
            System.out.println("📤 Published message to " + topic + ": " + message);
        } else {
            System.err.println("⚠️ MQTT client not connected, cannot publish!");
        }
    }

    public static Map<String, JsonObject> getAllStatuses() {
        return sensorStatusMap;
    }

    public static JsonObject getStatusFor(String deviceId) {
        return sensorStatusMap.get(deviceId);
    }
}