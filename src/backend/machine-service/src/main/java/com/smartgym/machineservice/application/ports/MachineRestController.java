package com.smartgym.machineservice.application.ports;

import com.smartgym.machineservice.model.ConfigureMachineMessage;
import com.smartgym.machineservice.model.EndMachineSessionMessage;
import com.smartgym.machineservice.model.SetMachineMaintenanceMessage;
import com.smartgym.machineservice.model.StartMachineSessionMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.concurrent.CompletableFuture;

/**
 * Port per il layer REST del microservizio Machine.
 * Definisce gli endpoint HTTP esposti per la gestione delle machine
 */
public interface MachineRestController {

	CompletableFuture<ResponseEntity<?>> createMachine(@RequestBody ConfigureMachineMessage message);

	CompletableFuture<ResponseEntity<?>> updateMachine(@PathVariable String machineId, @RequestBody ConfigureMachineMessage message);

	CompletableFuture<ResponseEntity<?>> startMachineSession(@RequestBody StartMachineSessionMessage message);

	CompletableFuture<ResponseEntity<?>> endMachineSession(@RequestBody EndMachineSessionMessage message);

	CompletableFuture<ResponseEntity<?>> setMachineMaintenance(@RequestBody SetMachineMaintenanceMessage message);

	CompletableFuture<ResponseEntity<?>> getAllMachines();

	CompletableFuture<ResponseEntity<?>> getMachineStatus(@PathVariable String machineId);

	CompletableFuture<ResponseEntity<?>> getMachineHistory(@PathVariable String machineId);

}
