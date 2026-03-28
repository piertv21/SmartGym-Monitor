package com.smartgym.areaservice.application.ports;

import com.smartgym.areaservice.model.AreaAccessMessage;
import com.smartgym.areaservice.model.UpdateAreaCapacityMessage;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;

public interface AreaRestController {

    CompletableFuture<ResponseEntity<?>> processAreaAccess(AreaAccessMessage message);

    CompletableFuture<ResponseEntity<?>> getAreaById(String areaId);

    CompletableFuture<ResponseEntity<?>> getAllAreas();

    CompletableFuture<ResponseEntity<?>> updateAreaCapacity(UpdateAreaCapacityMessage message);
}