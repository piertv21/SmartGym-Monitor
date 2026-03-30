package com.smartgym.areaservice.application.ports;

import com.smartgym.areaservice.model.GymArea;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AreaRepository {

    CompletableFuture<Void> saveArea(GymArea area);

    CompletableFuture<Optional<GymArea>> findAreaById(String areaId);

    CompletableFuture<List<GymArea>> findAllAreas();
}