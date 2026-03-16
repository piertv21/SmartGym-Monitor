package com.smartgym.embeddedservice.infrastracture.eventbus;


import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.model.NfcReaderMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class NfcVerticle extends AbstractVerticle {

    private EmbeddedServiceAPI embeddedService;
    private WebClient webClient;
    private static final String EMBEDDED_SERVICE_URL = "http://embedded-service:8086";
    private static final String PAYMENT_SERVICE_URL = "http://payment-service:8083";

    public NfcVerticle(EmbeddedServiceAPI embeddedService) {
        this.embeddedService = embeddedService;
    }

    @Override
    public void start() {
        webClient = WebClient.create(vertx);
        vertx.eventBus().<NfcReaderMessage>consumer("event.nfc", msg -> {
            JsonObject json = JsonObject.mapFrom(msg.body());
            NfcReaderMessage nfcMessage = json.mapTo(NfcReaderMessage.class);
            System.out.println("[NfcVerticle] Nfc detection event: " + nfcMessage);
            embeddedService.registerEvent("nfc", json)
                    .exceptionally(ex -> {
                        System.err.println("❌ Failed to save nfc event: " + ex.getMessage());
                        return null;
                    });

            String urlCheckPending = EMBEDDED_SERVICE_URL + "/getPendingPayment";

            webClient.getAbs(urlCheckPending)
                    .as(BodyCodec.jsonObject())
                    .send(ar -> {

                        if (ar.failed()) {
                            System.err.println("❌ Failed contacting Payment-Service: " + ar.cause());
                            return;
                        }
                        JsonObject pendingResponse = ar.result().body();
                        System.out.println(pendingResponse.encodePrettily());
                        JsonObject inner = pendingResponse.getJsonObject("map");
                        String plate = inner.getString("associatedWithPlate");
                        System.out.println("Plate waiting to be paid: "+ plate);
                        String processPayment = PAYMENT_SERVICE_URL + "/processPayment/" + nfcMessage.getNfcData();

                        String deletePending = EMBEDDED_SERVICE_URL + "/deletePendingPayment";

                        webClient.postAbs(processPayment)
                                .as(BodyCodec.jsonObject())
                                .sendJson(inner, response -> {
                                    if (response.failed()) {
                                        System.err.println("❌ Failed contacting Payment-Service: " + ar.cause());
                                        return;
                                    }
                                    JsonObject processPaymentResponse = ar.result().body();
                                    System.out.println(processPaymentResponse.encodePrettily());

                                    webClient.getAbs(deletePending)
                                            .as(BodyCodec.jsonObject())
                                            .sendJson(inner, deleteresponse -> {
                                                if (deleteresponse.failed()) {
                                                    System.err.println("❌ Failed contacting EMBEDDED-Service FOR DELETE: " + ar.cause());
                                                    return;
                                                }
                                            });

                                });

                    });
        });


    }
}