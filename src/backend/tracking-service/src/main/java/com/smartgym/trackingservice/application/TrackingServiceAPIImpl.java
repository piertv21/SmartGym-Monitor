package com.smartgym.trackingservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgym.trackingservice.application.ports.TrackingRepository;
import com.smartgym.trackingservice.application.ports.TrackingServiceAPI;
import com.smartgym.trackingservice.application.ports.DummyServicePort;

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
public class TrackingServiceAPIImpl implements TrackingServiceAPI {

    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceAPIImpl.class);
    private static final DateTimeFormatter CREATED_AT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TrackingRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DummyServicePort dummyServicePort;

    public TrackingServiceAPIImpl(TrackingRepository repository, DummyServicePort dummyServicePort) {
        this.repository = repository;
        this.dummyServicePort = dummyServicePort;
    }

}
