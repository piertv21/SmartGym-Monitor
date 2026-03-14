package com.smartgym.trackingservice.application;

import com.smartgym.trackingservice.application.ports.TrackingRestController;
import com.smartgym.trackingservice.application.ports.TrackingServiceAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementazione del REST controller per Tracking Service.
 * Gestisce le richieste HTTP per la generazione e recupero dei report.
 */
@RestController
public class TrackingRestControllerImpl implements TrackingRestController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingRestControllerImpl.class);

    private final TrackingServiceAPI trackingService;

    public TrackingRestControllerImpl(TrackingServiceAPI trackingService) {
        this.trackingService = trackingService;
        logger.info("✅ TrackingRestControllerImpl initialized");
    }



}
