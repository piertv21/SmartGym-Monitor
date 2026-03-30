package com.smartgym.embeddedservice.application.ports;

import io.vertx.core.json.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

public interface EmbeddedRestController {

    //qui definire i metodi REST per esporre le funzionalità dell'EmbeddedServiceAPI , al momento
    //non ce ne sono perchè si limita ad essere un Consumer di messsaggi MQTT
    //eventualmente si possono aggiungere metodi per esporre dati o funzionalità specifiche dell'embedded service,
    // ad esempio per monitorare lo stato del servizio o per recuperare dati storici degli eventi processati

}

