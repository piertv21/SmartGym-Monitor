package com.smartgym.analyticsservice.application;

import com.smartgym.analyticsservice.application.ports.AnalyticsRestController;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;

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
 * Implementazione del REST controller per Analytics Service.
 * Gestisce le richieste HTTP per la generazione e recupero dei report.
 */
@RestController
public class AnalyticsRestControllerImpl implements AnalyticsRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsRestControllerImpl.class);

    private final AnalyticsServiceAPI analyticsService;

    public AnalyticsRestControllerImpl(AnalyticsServiceAPI analyticsService) {
        this.analyticsService = analyticsService;
        logger.info("✅ AnalyticsRestControllerImpl initialized");
    }



}
