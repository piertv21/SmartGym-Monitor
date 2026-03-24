//package com.smartgym.ticketingservice;
//
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//
//import io.vertx.core.Vertx;
//import io.vertx.core.json.JsonObject;
//import io.vertx.junit5.VertxExtension;
//import io.vertx.junit5.VertxTestContext;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//@ExtendWith(VertxExtension.class)
//public class MqttMessageTest {
//
//    @Test
//    void testCoilDetectionMessageHandled(Vertx vertx, VertxTestContext testContext) {
//        String address = "event.coil";
//        CoilDetectionMessage msg = new CoilDetectionMessage(
//                "esp32-coil-01",
//                "2025-10-31T23:55:00Z",
//                "entry",
//                "occupied"
//        );
//        msg.setDeviceId("esp32-coil-01");
//
//        vertx.eventBus().consumer(address, message -> {
//            JsonObject body = (JsonObject) message.body();
//            CoilDetectionMessage received = body.mapTo(CoilDetectionMessage.class);
//
//            testContext.verify(() -> {
//                assertEquals("esp32-coil-01", received.getDeviceId());
//                assertEquals("entry", received.getCoilAt());
//                assertEquals("occupied", received.getStatus());
//                assertNotNull(received.getTimestamp());
//            });
//            testContext.completeNow();
//        });
//
//        vertx.eventBus().publish(address, JsonObject.mapFrom(msg));
//    }
//
//    @Test
//    void testPlateDetectionMessageHandled(Vertx vertx, VertxTestContext testContext) {
//        String address = "event.camera";
//        PlateDetectionMessage msg = new PlateDetectionMessage(
//                "2025-10-31T23:45:00Z",
//                "2025-10-31",
//                "esp32-cam-01",
//                "AB123CD"
//        );
//
//        vertx.eventBus().consumer(address, message -> {
//            JsonObject body = (JsonObject) message.body();
//            PlateDetectionMessage received = body.mapTo(PlateDetectionMessage.class);
//
//            testContext.verify(() -> {
//                assertEquals("esp32-cam-01", received.getDeviceId());
//                assertEquals("AB123CD", received.getLicensePlate());
//                assertEquals("2025-10-31", received.getDate());
//            });
//            testContext.completeNow();
//        });
//
//        vertx.eventBus().publish(address, JsonObject.mapFrom(msg));
//    }
//
//    @Test
//    void testNfcReaderMessageHandled(Vertx vertx, VertxTestContext testContext) {
//        String address = "event.nfc";
//        NfcReaderMessage msg = new NfcReaderMessage(
//                "2025-10-31T23:50:00Z",
//                "2025-10-31",
//                "esp32-nfc-02",
//                "04AABB22FF11"
//        );
//
//        vertx.eventBus().consumer(address, message -> {
//            JsonObject body = (JsonObject) message.body();
//            NfcReaderMessage received = body.mapTo(NfcReaderMessage.class);
//
//            testContext.verify(() -> {
//                assertEquals("esp32-nfc-02", received.getDeviceId());
//                assertEquals("04AABB22FF11", received.getNfcData());
//            });
//            testContext.completeNow();
//        });
//
//        vertx.eventBus().publish(address, JsonObject.mapFrom(msg));
//    }
//
//    @Test
//    void testHelpButtonMessageHandled(Vertx vertx, VertxTestContext testContext) {
//        String address = "event.helpbutton";
//        HelpButtonMessage msg = new HelpButtonMessage(
//                "2025-10-31T23:59:30Z",
//                "esp32-help-01",
//                "pressed"
//        );
//
//        vertx.eventBus().consumer(address, message -> {
//            JsonObject body = (JsonObject) message.body();
//            HelpButtonMessage received = body.mapTo(HelpButtonMessage.class);
//
//            testContext.verify(() -> {
//                assertEquals("esp32-help-01", received.getDeviceId());
//                assertEquals("pressed", received.getValue());
//            });
//            testContext.completeNow();
//        });
//
//        vertx.eventBus().publish(address, JsonObject.mapFrom(msg));
//    }
//}