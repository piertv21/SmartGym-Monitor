package com.smartgym.embeddedservice.application.ports;

import com.smartgym.embeddedservice.model.GymAccessMessage;
import java.util.concurrent.CompletableFuture;

public interface TrackingServicePort {

    CompletableFuture<Void> startGymSession(GymAccessMessage message);

    CompletableFuture<Void> endGymSession(GymAccessMessage message);
}
