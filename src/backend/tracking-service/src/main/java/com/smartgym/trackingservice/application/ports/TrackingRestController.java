package com.smartgym.trackingservice.application.ports;

import com.smartgym.trackingservice.model.EndGymSessionMessage;
import com.smartgym.trackingservice.model.StartGymSessionMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.concurrent.CompletableFuture;

public interface TrackingRestController {

	CompletableFuture<ResponseEntity<?>> startGymSession(@RequestBody StartGymSessionMessage message);

	CompletableFuture<ResponseEntity<?>> endGymSession(@RequestBody EndGymSessionMessage message);

	CompletableFuture<ResponseEntity<?>> getGymCount();

	CompletableFuture<ResponseEntity<?>> getActiveSessions();
}