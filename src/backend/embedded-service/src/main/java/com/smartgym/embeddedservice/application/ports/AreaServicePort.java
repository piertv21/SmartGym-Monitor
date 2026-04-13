package com.smartgym.embeddedservice.application.ports;

import com.smartgym.embeddedservice.model.AreaAccessMessage;
import java.util.concurrent.CompletableFuture;

public interface AreaServicePort {

    CompletableFuture<Void> processAreaAccess(AreaAccessMessage message);

    CompletableFuture<Void> processAreaExit(AreaAccessMessage message);
}
