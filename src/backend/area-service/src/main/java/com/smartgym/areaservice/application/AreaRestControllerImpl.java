package com.smartgym.areaservice.application;

import com.smartgym.areaservice.application.ports.AreaRestController;
import com.smartgym.areaservice.application.ports.AreaServiceAPI;

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
 * Implementazione del REST controller per Area Service.
 * Gestisce le richieste HTTP per la generazione e recupero dei report.
 */
@RestController
public class AreaRestControllerImpl implements AreaRestController {

    private static final Logger logger = LoggerFactory.getLogger(AreaRestControllerImpl.class);

    private final AreaServiceAPI areaService;

    public AreaRestControllerImpl(AreaServiceAPI areaService) {
        this.areaService = areaService;
        logger.info("✅ AreaRestControllerImpl initialized");
    }



}
