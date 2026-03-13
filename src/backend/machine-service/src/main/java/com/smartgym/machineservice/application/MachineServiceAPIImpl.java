package com.smartgym.machineservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.machineservice.application.ports.MachineRepository;
import com.smartgym.machineservice.application.ports.MachineServiceAPI;
import com.smartgym.machineservice.application.ports.DummyServicePort;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service
public class MachineServiceAPIImpl implements MachineServiceAPI {

    private static final Logger logger = LoggerFactory.getLogger(MachineServiceAPIImpl.class);
    private static final DateTimeFormatter CREATED_AT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MachineRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DummyServicePort dummyServicePort;

    public MachineServiceAPIImpl(MachineRepository repository, DummyServicePort dummyServicePort) {
        this.repository = repository;
        this.dummyServicePort = dummyServicePort;
    }

}
