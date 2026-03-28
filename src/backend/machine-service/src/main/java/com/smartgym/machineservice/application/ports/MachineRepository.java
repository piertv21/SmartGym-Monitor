package com.smartgym.machineservice.application.ports;

import com.smartgym.machineservice.model.Machine;
import com.smartgym.machineservice.model.MachineSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port per l'accesso ai dati
 *
 */
public interface MachineRepository {

	CompletableFuture<Void> saveMachine(Machine machine);

	CompletableFuture<Optional<Machine>> findMachineById(String machineId);

	CompletableFuture<Void> saveMachineSession(MachineSession session);

	CompletableFuture<Optional<MachineSession>> findActiveSessionByMachineId(String machineId);

	CompletableFuture<List<MachineSession>> findMachineHistoryByMachineId(String machineId);

}
