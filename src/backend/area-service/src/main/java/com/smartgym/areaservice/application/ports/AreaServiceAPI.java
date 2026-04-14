package com.smartgym.areaservice.application.ports;

import com.smartgym.areaservice.ddd.Service;
import com.smartgym.areaservice.model.AreaAccessMessage;
import com.smartgym.areaservice.model.GymArea;
import com.smartgym.areaservice.model.UpdateAreaCapacityMessage;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AreaServiceAPI extends Service {

    CompletableFuture<Void> processAreaAccess(AreaAccessMessage message);

    CompletableFuture<Void> processAreaExit(AreaAccessMessage message);

    CompletableFuture<Optional<GymArea>> getAreaById(String areaId);

    CompletableFuture<List<GymArea>> getAllAreas();

    CompletableFuture<Void> updateAreaCapacity(UpdateAreaCapacityMessage message);
}
