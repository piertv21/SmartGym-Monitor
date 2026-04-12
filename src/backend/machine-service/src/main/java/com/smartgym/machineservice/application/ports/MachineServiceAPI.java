package com.smartgym.machineservice.application.ports;

import com.smartgym.machineservice.ddd.Service;
import com.smartgym.machineservice.model.ConfigureMachineMessage;
import com.smartgym.machineservice.model.EndMachineSessionMessage;
import com.smartgym.machineservice.model.Machine;
import com.smartgym.machineservice.model.MachineSession;
import com.smartgym.machineservice.model.MachineUsageSeriesResponse;
import com.smartgym.machineservice.model.SetMachineMaintenanceMessage;
import com.smartgym.machineservice.model.StartMachineSessionMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MachineServiceAPI extends Service {


	CompletableFuture<Machine> createMachine(ConfigureMachineMessage message);

	CompletableFuture<Machine> updateMachine(String machineId, ConfigureMachineMessage message);

	CompletableFuture<MachineSession> startMachineSession(StartMachineSessionMessage message);

	CompletableFuture<MachineSession> endMachineSession(EndMachineSessionMessage message);

	CompletableFuture<List<Machine>> getAllMachines();

	CompletableFuture<Machine> setMachineMaintenance(SetMachineMaintenanceMessage message);

	CompletableFuture<Machine> getMachineStatus(String machineId);

	CompletableFuture<List<MachineSession>> getMachineHistory(String machineId);

	CompletableFuture<MachineUsageSeriesResponse> getMachineUsageSeries(String from, String to, String granularity, String areaId, String machineId);

}
