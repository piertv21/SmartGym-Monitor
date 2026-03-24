package com.smartgym.embeddedservice.application;

import com.smartgym.embeddedservice.application.ports.EmbeddedRestController;
import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmbeddedRestControllerImpl implements EmbeddedRestController {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedRestControllerImpl.class);
    private final MqttManager mqttManager;
    private final EmbeddedServiceAPI embeddedService;

    public EmbeddedRestControllerImpl(MqttManager mqttManager, EmbeddedServiceAPI embeddedService) {
        this.mqttManager = mqttManager;
        this.embeddedService = embeddedService;
    }


}
