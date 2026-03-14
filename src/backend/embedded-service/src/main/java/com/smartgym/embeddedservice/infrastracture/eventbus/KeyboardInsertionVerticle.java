package com.smartgym.embeddedservice.infrastracture.eventbus;

import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.CoilDetectionMessage;
import com.smartgym.embeddedservice.model.KeyboardPlateInsertionMessage;
import com.smartgym.embeddedservice.model.TicketMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class KeyboardInsertionVerticle extends AbstractVerticle {

    private final EmbeddedServiceAPI embeddedService;
    private WebClient webClient;
    private static final String TICKETING_SERVICE_URL = "http://ticketing-service:8084";
    private static final String PAYMENT_SERVICE_URL = "http://payment-service:8083";
    private static final String EMBEDDED_SERVICE_URL = "http://embedded-service:8086";

    public KeyboardInsertionVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        webClient = WebClient.create(vertx);
        vertx.eventBus().<CoilDetectionMessage>consumer("event.keyboardInsertion", msg -> {
            JsonObject json = JsonObject.mapFrom(msg.body());
            KeyboardPlateInsertionMessage keyboardInsertion = json.mapTo(KeyboardPlateInsertionMessage.class); //è la targa
            System.out.println("[KeyboardPlateInsertion] Keyboard insertion detection event: " + keyboardInsertion);
            embeddedService.registerEvent("keyboard_insertion", json)
            .exceptionally(ex -> {
                System.err.println("❌ Failed to save keyboard_insertion event: " + ex.getMessage());
                return null;
            });

            String plate = keyboardInsertion.getPlate();
            String urlTicketing = TICKETING_SERVICE_URL + "/getTicketByPlate/" + plate;
            String urlPayment =  PAYMENT_SERVICE_URL + "/calculateFee";
            String urlEnableNfc =  EMBEDDED_SERVICE_URL + "/nfc/enable/";

            webClient.getAbs(urlTicketing)
                    .as(BodyCodec.jsonObject())
                    .send(ar -> {
                        if (ar.succeeded()) {
                            JsonObject responseTicket = ar.result().body();
                            if (responseTicket ==  null) {
                                System.err.println("❌ Failed to retrieve Ticket (Ticket doesn't exist)");
                                return;
                            }
                            System.out.println("✅ Ticket retrieved for plate " + plate + ": " + responseTicket.encodePrettily());

                            TicketMessage ticketMsg;
                            try {
                                ticketMsg = responseTicket.mapTo(TicketMessage.class);
                            } catch (Exception e) {
                                System.err.println("❌ Failed to map Ticket JSON into TicketMessage: "
                                        + e.getMessage());
                                return;
                            }

                            webClient.postAbs(urlPayment)
                                    .putHeader("Content-Type", "application/json")
                                    .as(BodyCodec.jsonObject())
                                    .sendJson(ticketMsg, paymentAr -> {
                                        if (paymentAr.succeeded()) {
                                            JsonObject responsePayment = paymentAr.result().body();
                                            System.out.println("💰 Fee calculation response: " + responsePayment.encodePrettily());
                                            webClient.postAbs(urlEnableNfc + plate)
                                                    .putHeader("Content-Type", "application/json")
                                                    .as(BodyCodec.jsonObject())
                                                    .sendJson(ticketMsg,embeddedAr-> {
                                                        if (embeddedAr.succeeded()) {
                                                            JsonObject responseEmbedded = embeddedAr.result().body();
                                                            System.out.println("Embedded Response: " + responseEmbedded.encodePrettily() + " for plate " + ticketMsg.getAssociatedWithPlate());
                                                        }
                                                    });
                                        } else {
                                            System.err.println("❌ Failed to contact Payment-Service: "
                                                    + paymentAr.cause().getMessage());
                                        }
                                    });

                        } else {
                            System.err.println("❌ Failed to contact Ticketing-Service: " + ar.cause().getMessage());

                        }
                    });
        });
    }
}
