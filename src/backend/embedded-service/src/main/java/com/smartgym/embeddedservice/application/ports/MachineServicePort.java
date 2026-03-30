package com.smartgym.embeddedservice.application.ports;

import com.smartgym.embeddedservice.model.MachineUsageMessage;

import java.util.concurrent.CompletableFuture;

public interface MachineServicePort {

    CompletableFuture<Void> startSession(MachineUsageMessage message);

    CompletableFuture<Void> endSession(MachineUsageMessage message);
}
