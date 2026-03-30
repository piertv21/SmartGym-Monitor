package com.smartgym.areaservice.application;

import com.smartgym.areaservice.application.ports.AreaRepository;
import com.smartgym.areaservice.application.ports.AreaServiceAPI;
import com.smartgym.areaservice.model.AreaAccessMessage;
import com.smartgym.areaservice.model.GymArea;
import com.smartgym.areaservice.model.UpdateAreaCapacityMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AreaServiceAPIImpl implements AreaServiceAPI {

    private static final Logger logger = LoggerFactory.getLogger(AreaServiceAPIImpl.class);

    private final AreaRepository areaRepository;

    public AreaServiceAPIImpl(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @Override
    public CompletableFuture<Void> processAreaAccess(AreaAccessMessage message) {
        return CompletableFuture.runAsync(() -> {
            validateAreaAccessMessage(message);

            GymArea area = areaRepository.findAreaById(message.getAreaId()).join()
                    .orElseThrow(() -> new IllegalArgumentException("Area not found: " + message.getAreaId()));

            if (message.isEntry()) {
                area.incrementCount();
            } else if (message.isExit()) {
                if (area.getCurrentCount() == null || area.getCurrentCount() <= 0) {
                    logger.warn("OUT ignored at zero: areaId={}, badgeId={}, count={}",
                            message.getAreaId(), message.getBadgeId(), area.getCurrentCount());
                }
                area.decrementCount();
            } else {
                throw new IllegalArgumentException("Invalid direction: " + message.getDirection());
            }

            areaRepository.saveArea(area).join();
        });
    }

    @Override
    public CompletableFuture<Void> processAreaExit(AreaAccessMessage message) {
        return CompletableFuture.runAsync(() -> {
            validateAreaAccessMessage(message);

            GymArea area = areaRepository.findAreaById(message.getAreaId()).join()
                    .orElseThrow(() -> new IllegalArgumentException("Area not found: " + message.getAreaId()));

            if (area.getCurrentCount() == null || area.getCurrentCount() <= 0) {
                logger.warn("OUT ignored at zero (exit endpoint): areaId={}, badgeId={}, count={}",
                        message.getAreaId(), message.getBadgeId(), area.getCurrentCount());
            }

            area.decrementCount();

            areaRepository.saveArea(area).join();
        });
    }

    @Override
    public CompletableFuture<Optional<GymArea>> getAreaById(String areaId) {
        if (isBlank(areaId)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("areaId cannot be null or empty")
            );
        }

        return areaRepository.findAreaById(areaId);
    }

    @Override
    public CompletableFuture<List<GymArea>> getAllAreas() {
        return areaRepository.findAllAreas();
    }

    @Override
    public CompletableFuture<Void> updateAreaCapacity(UpdateAreaCapacityMessage message) {
        return CompletableFuture.runAsync(() -> {
            validateUpdateCapacityMessage(message);

            GymArea area = areaRepository.findAreaById(message.getAreaId()).join()
                    .orElseThrow(() -> new IllegalArgumentException("Area not found: " + message.getAreaId()));

            if (message.getCapacity() < 0) {
                throw new IllegalArgumentException("Capacity cannot be negative");
            }

            if (area.getCurrentCount() != null && message.getCapacity() < area.getCurrentCount()) {
                throw new IllegalStateException("New capacity cannot be lower than current count");
            }

            area.setCapacity(message.getCapacity());
            areaRepository.saveArea(area).join();
        });
    }

    private void validateAreaAccessMessage(AreaAccessMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("AreaAccessMessage cannot be null");
        }
        if (isBlank(message.getAreaId())) {
            throw new IllegalArgumentException("areaId cannot be null or empty");
        }
        if (isBlank(message.getBadgeId())) {
            throw new IllegalArgumentException("badgeId cannot be null or empty");
        }
        if (isBlank(message.getDirection())) {
            throw new IllegalArgumentException("direction cannot be null or empty");
        }
    }

    private void validateUpdateCapacityMessage(UpdateAreaCapacityMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("UpdateAreaCapacityMessage cannot be null");
        }
        if (isBlank(message.getAreaId())) {
            throw new IllegalArgumentException("areaId cannot be null or empty");
        }
        if (message.getCapacity() == null) {
            throw new IllegalArgumentException("capacity cannot be null");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}