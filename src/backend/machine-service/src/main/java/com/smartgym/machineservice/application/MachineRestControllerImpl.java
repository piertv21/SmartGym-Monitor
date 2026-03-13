package com.smartgym.machineservice.application;

import com.smartgym.machineservice.application.ports.MachineRestController;
import com.smartgym.machineservice.application.ports.MachineServiceAPI;

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
 * Implementazione del REST controller per Machine Service.
 * Gestisce le richieste HTTP per la generazione e recupero dei report.
 */
@RestController
public class MachineRestControllerImpl implements MachineRestController {

    private static final Logger logger = LoggerFactory.getLogger(MachineRestControllerImpl.class);

    private final MachineServiceAPI machineService;

    public MachineRestControllerImpl(MachineServiceAPI machineService) {
        this.machineService = machineService;
        logger.info("✅ MachineRestControllerImpl initialized");
    }



}
