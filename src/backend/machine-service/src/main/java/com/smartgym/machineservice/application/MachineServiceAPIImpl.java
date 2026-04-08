package com.smartgym.machineservice.application;

import com.smartgym.machineservice.application.ports.MachineRepository;
import com.smartgym.machineservice.application.ports.MachineServiceAPI;
import com.smartgym.machineservice.model.ConfigureMachineMessage;
import com.smartgym.machineservice.model.EndMachineSessionMessage;
import com.smartgym.machineservice.model.Machine;
import com.smartgym.machineservice.model.MachineSession;
import com.smartgym.machineservice.model.OccupancyStatus;
import com.smartgym.machineservice.model.Sensor;
import com.smartgym.machineservice.model.SetMachineMaintenanceMessage;
import com.smartgym.machineservice.model.StartMachineSessionMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MachineServiceAPIImpl implements MachineServiceAPI {

    private final MachineRepository repository;

    public MachineServiceAPIImpl(MachineRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Machine> configureMachine(ConfigureMachineMessage message) {
        return createMachine(message);
    }

    @Override
    public CompletableFuture<Machine> createMachine(ConfigureMachineMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            validateConfigureMessage(message);

            repository.findMachineById(message.getMachineId()).join()
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Machine already exists: " + existing.getMachineId());
                    });

            Machine machine = new Machine(
                    message.getMachineId(),
                    message.getAreaId(),
                    OccupancyStatus.FREE,
                    new Sensor(message.getSensor().trim())
            );

            repository.saveMachine(machine).join();
            return machine;
        });
    }

    @Override
    public CompletableFuture<Machine> updateMachine(String machineId, ConfigureMachineMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            validateMachineId(machineId);
            validateUpdateMessage(message);

            if (!isBlank(message.getMachineId()) && !machineId.equals(message.getMachineId().trim())) {
                throw new IllegalArgumentException("machineId in path and body must match");
            }

            Machine existing = getRequiredMachine(machineId);
            Machine updated = new Machine(
                    existing.getMachineId(),
                    message.getAreaId(),
                    existing.getStatus(),
                    existing.getActiveSessionId(),
                    new Sensor(message.getSensor().trim())
            );

            repository.saveMachine(updated).join();
            return updated;
        });
    }

    @Override
    public CompletableFuture<MachineSession> startMachineSession(StartMachineSessionMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            validateStartMessage(message);

            Machine machine = getRequiredMachine(message.getMachineId());

            repository.findActiveSessionByMachineId(machine.getMachineId()).join()
                    .ifPresent(session -> {
                        throw new IllegalStateException("Machine already has an active session: " + machine.getMachineId());
                    });

            String sessionId = UUID.randomUUID().toString();
            MachineSession session = new MachineSession(
                    sessionId,
                    machine.getMachineId(),
                    message.getBadgeId(),
                    LocalDateTime.now()
            );

            machine.startSession(session.getSessionId());

            repository.saveMachineSession(session).join();
            repository.saveMachine(machine).join();

            return session;
        });
    }

    @Override
    public CompletableFuture<MachineSession> endMachineSession(EndMachineSessionMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            validateEndMessage(message);

            Machine machine = getRequiredMachine(message.getMachineId());
            MachineSession session = repository.findActiveSessionByMachineId(machine.getMachineId()).join()
                    .orElseThrow(() -> new IllegalStateException("Machine has no active session: " + machine.getMachineId()));

            session.end(LocalDateTime.now());
            machine.endSession(session.getSessionId());

            repository.saveMachineSession(session).join();
            repository.saveMachine(machine).join();

            return session;
        });
    }

    @Override
    public CompletableFuture<List<Machine>> getAllMachines() {
        return repository.findAllMachines();
    }

    @Override
    public CompletableFuture<Machine> setMachineMaintenance(SetMachineMaintenanceMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            validateMaintenanceMessage(message);

            Machine machine = getRequiredMachine(message.getMachineId());
            boolean active = message.getActive() != null ? message.getActive() : true;
            
            if (active) {
                machine.setMaintenance();
            } else {
                machine.setAvailable();
            }
            
            repository.saveMachine(machine).join();
            return machine;
        });
    }

    @Override
    public CompletableFuture<Machine> getMachineStatus(String machineId) {
        validateMachineId(machineId);

        return repository.findMachineById(machineId)
                .thenApply(machineOpt -> machineOpt.orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + machineId)));
    }

    @Override
    public CompletableFuture<List<MachineSession>> getMachineHistory(String machineId) {
        validateMachineId(machineId);

        return repository.findMachineHistoryByMachineId(machineId);
    }

    private Machine getRequiredMachine(String machineId) {
        return repository.findMachineById(machineId).join()
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + machineId));
    }

    private void validateConfigureMessage(ConfigureMachineMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("ConfigureMachineMessage cannot be null");
        }
        if (isBlank(message.getMachineId())) {
            throw new IllegalArgumentException("machineId cannot be null or empty");
        }
        if (isBlank(message.getAreaId())) {
            throw new IllegalArgumentException("areaId cannot be null or empty");
        }
        if (isBlank(message.getSensor())) {
            throw new IllegalArgumentException("sensor cannot be null or empty");
        }
    }

    private void validateUpdateMessage(ConfigureMachineMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("ConfigureMachineMessage cannot be null");
        }
        if (isBlank(message.getAreaId())) {
            throw new IllegalArgumentException("areaId cannot be null or empty");
        }
        if (isBlank(message.getSensor())) {
            throw new IllegalArgumentException("sensor cannot be null or empty");
        }
    }

    private void validateStartMessage(StartMachineSessionMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("StartMachineSessionMessage cannot be null");
        }
        if (isBlank(message.getMachineId())) {
            throw new IllegalArgumentException("machineId cannot be null or empty");
        }
        if (isBlank(message.getBadgeId())) {
            throw new IllegalArgumentException("badgeId cannot be null or empty");
        }
    }

    private void validateEndMessage(EndMachineSessionMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("EndMachineSessionMessage cannot be null");
        }
        if (isBlank(message.getMachineId())) {
            throw new IllegalArgumentException("machineId cannot be null or empty");
        }
    }

    private void validateMaintenanceMessage(SetMachineMaintenanceMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("SetMachineMaintenanceMessage cannot be null");
        }
        if (isBlank(message.getMachineId())) {
            throw new IllegalArgumentException("machineId cannot be null or empty");
        }
    }

    private void validateMachineId(String machineId) {
        if (isBlank(machineId)) {
            throw new IllegalArgumentException("machineId cannot be null or empty");
        }
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }

}
