package com.smartgym.embeddedservice.infrastracture.adapters.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.embeddedservice.infrastracture.config.MqttConfig;
import com.smartgym.embeddedservice.model.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VertxMqttClientAdapter extends AbstractVerticle {

    private static MqttClient clientInstance;

    private static final Map<String, JsonObject> sensorStatusMap = new ConcurrentHashMap<>();

    @Override
    public void start() {

        MqttClientOptions options = new MqttClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setUsername(MqttConfig.MQTT_USERNAME)
                .setPassword(MqttConfig.MQTT_PASSWORD);

        MqttClient client = MqttClient.create(vertx, options);
        ObjectMapper mapper = new ObjectMapper();

        client.connect(MqttConfig.BROKER_PORT, MqttConfig.BROKER_HOST, c -> {
            if (c.succeeded()) {
                System.out.println("✅ Connected to MQTT broker");
                clientInstance = client;
                client.subscribe("parking/sensor/#", 1);
            } else {
                System.err.println("❌ MQTT connection failed: " + c.cause().getMessage());
            }
        });

        client.publishHandler(msg -> {
            String topic = msg.topicName();
            String payload = msg.payload().toString();

            System.out.println("📩 Received message on topic: " + topic);

            try {
                switch (topic) {
                    case "parking/sensor/platedetection" -> {
                        PlateDetectionMessage cam = mapper.readValue(payload, PlateDetectionMessage.class);
                        vertx.eventBus().publish("event.camera", JsonObject.mapFrom(cam));
                        return;
                    }
                    case "parking/sensor/coildetection" -> {
                        CoilDetectionMessage coil = mapper.readValue(payload, CoilDetectionMessage.class);
                        vertx.eventBus().publish("event.coil", JsonObject.mapFrom(coil));
                        return;
                    }
                    case "parking/sensor/nfc" -> {
                        NfcReaderMessage nfc = mapper.readValue(payload, NfcReaderMessage.class);
                        vertx.eventBus().publish("event.nfc", JsonObject.mapFrom(nfc));
                        return;
                    }
                    case "parking/sensor/helpbutton" -> {
                        HelpButtonMessage help = mapper.readValue(payload, HelpButtonMessage.class);
                        vertx.eventBus().publish("event.helpbutton", JsonObject.mapFrom(help));
                        return;
                    }
                    case "parking/sensor/cmd" -> {
                        CmdCommandMessage cmd = mapper.readValue(payload, CmdCommandMessage.class);
                        vertx.eventBus().publish("event.cmd", JsonObject.mapFrom(cmd));
                        return;
                    }
                    case "parking/sensor/keyboardInsertion" -> {
                        KeyboardPlateInsertionMessage keyboardPlate = mapper.readValue(payload, KeyboardPlateInsertionMessage.class);
                        vertx.eventBus().publish("event.keyboardInsertion", JsonObject.mapFrom(keyboardPlate));
                        return;
                    }
                    default -> {
                        // Gestione dinamica di tutti i topic dei sensori con "/status"
                        if (topic.startsWith("parking/sensor/") && topic.endsWith("/status")) {
                            try {
                                StatusMessage status = mapper.readValue(payload, StatusMessage.class);
                                JsonObject json = JsonObject.mapFrom(status);
                                sensorStatusMap.put(status.getDeviceId(), json);
                                System.out.println("💾 Updated status for sensor [" + status.getDeviceId() + "]: " + json.encodePrettily());
                            } catch (Exception e) {
                                System.err.println("❌ Error parsing StatusMessage: " + e.getMessage());
                            }
                        } else {
                            System.out.println("⚠️ Unknown topic received: " + topic);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error parsing MQTT message: " + e.getMessage());
            }
        });
    }

    public static void publish(String topic, String message) {
        if (clientInstance != null && clientInstance.isConnected()) {
            clientInstance.publish(
                    topic,
                    io.vertx.core.buffer.Buffer.buffer(message),
                    io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE,
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

    public static JsonObject getStatusFor(String sensorId) {
        return sensorStatusMap.get(sensorId);
    }
}
