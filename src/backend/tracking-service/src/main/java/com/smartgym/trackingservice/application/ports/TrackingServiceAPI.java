package com.smartgym.trackingservice.application.ports;

import com.smartgym.trackingservice.ddd.Service;
import com.smartgym.trackingservice.model.EndGymSessionMessage;
import com.smartgym.trackingservice.model.GymSession;
import com.smartgym.trackingservice.model.StartGymSessionMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TrackingServiceAPI extends Service {

    CompletableFuture<GymSession> startGymSession(StartGymSessionMessage message);

    CompletableFuture<GymSession> endGymSession(EndGymSessionMessage message);

    CompletableFuture<Long> getGymCount();

    CompletableFuture<List<GymSession>> getActiveSessions();
}
