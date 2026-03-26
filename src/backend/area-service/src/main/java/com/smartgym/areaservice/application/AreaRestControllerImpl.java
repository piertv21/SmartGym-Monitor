package com.smartgym.areaservice.application;

import com.smartgym.areaservice.application.ports.AreaRestController;
import com.smartgym.areaservice.application.ports.AreaServiceAPI;
import com.smartgym.areaservice.model.AreaAccessMessage;
import com.smartgym.areaservice.model.UpdateAreaCapacityMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/area-service")
public class AreaRestControllerImpl implements AreaRestController {

    private final AreaServiceAPI areaServiceAPI;

    public AreaRestControllerImpl(AreaServiceAPI areaServiceAPI) {
        this.areaServiceAPI = areaServiceAPI;
    }

    @Override
    @PostMapping("/access")
    public CompletableFuture<ResponseEntity<?>> processAreaAccess(@RequestBody AreaAccessMessage message) {
        return areaServiceAPI.processAreaAccess(message)
                .thenApply(result -> ResponseEntity.ok(
                        Map.of(
                                "message", "Area access processed successfully",
                                "areaId", message.getAreaId(),
                                "badgeId", message.getBadgeId(),
                                "direction", message.getDirection()
                        )
                ));
    }

    @Override
    @GetMapping("/{areaId}")
    public CompletableFuture<ResponseEntity<?>> getAreaById(@PathVariable String areaId) {
        return areaServiceAPI.getAreaById(areaId)
                .thenApply(areaOpt -> areaOpt
                        .<ResponseEntity<?>>map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @Override
    @GetMapping
    public CompletableFuture<ResponseEntity<?>> getAllAreas() {
        return areaServiceAPI.getAllAreas()
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @PutMapping("/capacity")
    public CompletableFuture<ResponseEntity<?>> updateAreaCapacity(@RequestBody UpdateAreaCapacityMessage message) {
        return areaServiceAPI.updateAreaCapacity(message)
                .thenApply(result -> ResponseEntity.ok(
                        Map.of(
                                "message", "Area capacity updated successfully",
                                "areaId", message.getAreaId(),
                                "capacity", message.getCapacity()
                        )
                ));
    }
}