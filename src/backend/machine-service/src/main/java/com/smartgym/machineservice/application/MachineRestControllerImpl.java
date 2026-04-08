package com.smartgym.machineservice.application;

import com.smartgym.machineservice.application.ports.MachineRestController;
import com.smartgym.machineservice.application.ports.MachineServiceAPI;
import com.smartgym.machineservice.model.ConfigureMachineMessage;
import com.smartgym.machineservice.model.EndMachineSessionMessage;
import com.smartgym.machineservice.model.SetMachineMaintenanceMessage;
import com.smartgym.machineservice.model.StartMachineSessionMessage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementazione del REST controller per Machine Service.
 * Gestisce le richieste HTTP per la generazione e recupero dei report.
 */
@RestController
public class MachineRestControllerImpl implements MachineRestController {

    private final MachineServiceAPI machineService;

    public MachineRestControllerImpl(MachineServiceAPI machineService) {
        this.machineService = machineService;
    }

    @Override
    @PostMapping("/machines")
    public CompletableFuture<ResponseEntity<?>> createMachine(@RequestBody ConfigureMachineMessage message) {
        return machineService.createMachine(message)
                .thenApply(machine -> ResponseEntity.status(HttpStatus.CREATED).body(
                        Map.of(
                                "message", "Machine created successfully",
                                "machineId", machine.getMachineId(),
                                "areaId", machine.getAreaId(),
                                "status", machine.getStatus().name(),
                                "sensor", machine.getSensor().getId()
                        )
                ));
    }

    @Override
    @PutMapping("/machines/{machineId}")
    public CompletableFuture<ResponseEntity<?>> updateMachine(@PathVariable String machineId,
                                                               @RequestBody ConfigureMachineMessage message) {
        return machineService.updateMachine(machineId, message)
                .thenApply(machine -> ResponseEntity.ok(
                        Map.of(
                                "message", "Machine updated successfully",
                                "machineId", machine.getMachineId(),
                                "areaId", machine.getAreaId(),
                                "status", machine.getStatus().name(),
                                "sensor", machine.getSensor().getId()
                        )
                ));
    }

    @Override
    @PostMapping("/start-session")
    public CompletableFuture<ResponseEntity<?>> startMachineSession(@RequestBody StartMachineSessionMessage message) {
        return machineService.startMachineSession(message)
                .thenApply(session -> ResponseEntity.ok(
                        Map.of(
                                "message", "Machine session started",
                                "sessionId", session.getSessionId(),
                                "machineId", session.getMachineId(),
                                "badgeId", session.getBadgeId(),
                                "startTime", session.getStartTime().toString()
                        )
                ));
    }

    @Override
    @PostMapping("/end-session")
    public CompletableFuture<ResponseEntity<?>> endMachineSession(@RequestBody EndMachineSessionMessage message) {
        return machineService.endMachineSession(message)
                .thenApply(session -> ResponseEntity.ok(
                        Map.of(
                                "message", "Machine session ended",
                                "sessionId", session.getSessionId(),
                                "machineId", session.getMachineId(),
                                "endTime", String.valueOf(session.getEndTime())
                        )
                ));
    }

    @Override
    @PostMapping("/set-maintenance")
    public CompletableFuture<ResponseEntity<?>> setMachineMaintenance(@RequestBody SetMachineMaintenanceMessage message) {
        return machineService.setMachineMaintenance(message)
                .thenApply(machine -> ResponseEntity.ok(
                        Map.of(
                                "message", "Machine set to maintenance",
                                "machineId", machine.getMachineId(),
                                "status", machine.getStatus().name()
                        )
                ));
    }

    @Override
    @GetMapping("/machines")
    public CompletableFuture<ResponseEntity<?>> getAllMachines() {
        return machineService.getAllMachines()
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/{machineId}")
    public CompletableFuture<ResponseEntity<?>> getMachineStatus(@PathVariable String machineId) {
        return machineService.getMachineStatus(machineId)
                .thenApply(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/history/{machineId}")
    public CompletableFuture<ResponseEntity<?>> getMachineHistory(@PathVariable String machineId) {
        return machineService.getMachineHistory(machineId)
                .thenApply(ResponseEntity::ok);
    }
}