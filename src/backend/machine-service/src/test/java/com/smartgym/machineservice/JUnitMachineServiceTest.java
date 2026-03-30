package com.smartgym.machineservice;

import com.smartgym.machineservice.application.MachineServiceAPIImpl;
import com.smartgym.machineservice.application.ports.MachineRepository;
import com.smartgym.machineservice.model.ConfigureMachineMessage;
import com.smartgym.machineservice.model.EndMachineSessionMessage;
import com.smartgym.machineservice.model.Machine;
import com.smartgym.machineservice.model.MachineSession;
import com.smartgym.machineservice.model.OccupancyStatus;
import com.smartgym.machineservice.model.SetMachineMaintenanceMessage;
import com.smartgym.machineservice.model.StartMachineSessionMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JUnitMachineServiceTest {

    @Test
    void createMachineCreatesMachine() {
        MachineRepository repository = new InMemoryMachineRepository();
        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        Machine machine = service.createMachine(new ConfigureMachineMessage("m-01", "area-cardio", "sensor-m-01")).join();

        assertEquals("m-01", machine.getMachineId());
        assertEquals("area-cardio", machine.getAreaId());
        assertEquals(OccupancyStatus.FREE, machine.getStatus());
        assertEquals("sensor-m-01", machine.getSensor().getId());
    }

    @Test
    void createMachineFailsIfAlreadyExists() {
        InMemoryMachineRepository repository = new InMemoryMachineRepository();
        repository.saveMachine(new Machine("m-01", "area-cardio")).join();
        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> service.createMachine(new ConfigureMachineMessage("m-01", "area-weights", "sensor-m-02")).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    void updateMachineChangesAreaAndPreservesStatus() {
        InMemoryMachineRepository repository = new InMemoryMachineRepository();
        repository.saveMachine(new Machine("m-01", "area-cardio")).join();
        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        MachineSession session = service.startMachineSession(new StartMachineSessionMessage("m-01", "badge-01")).join();
        Machine updated = service.updateMachine("m-01", new ConfigureMachineMessage("m-01", "area-weights", "sensor-m-99")).join();

        assertEquals("area-weights", updated.getAreaId());
        assertEquals(OccupancyStatus.OCCUPIED, updated.getStatus());
        assertEquals(session.getSessionId(), updated.getActiveSessionId());
        assertEquals("sensor-m-99", updated.getSensor().getId());
    }

    @Test
    void startMachineSessionMarksMachineOccupied() {
        InMemoryMachineRepository repository = new InMemoryMachineRepository();
        repository.saveMachine(new Machine("m-01", "area-cardio")).join();
        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        MachineSession session = service.startMachineSession(new StartMachineSessionMessage("m-01", "badge-01")).join();

        assertNotNull(session.getSessionId());
        assertEquals("badge-01", session.getBadgeId());

        Machine status = service.getMachineStatus("m-01").join();
        assertEquals(OccupancyStatus.OCCUPIED, status.getStatus());
        assertEquals(session.getSessionId(), status.getActiveSessionId());
    }

    @Test
    void startMachineSessionFailsIfAlreadyOccupied() {
        InMemoryMachineRepository repository = new InMemoryMachineRepository();
        repository.saveMachine(new Machine("m-01", "area-cardio")).join();
        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        service.startMachineSession(new StartMachineSessionMessage("m-01", "badge-01")).join();

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> service.startMachineSession(new StartMachineSessionMessage("m-01", "badge-02")).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    void endMachineSessionClosesSessionAndFreesMachine() {
        InMemoryMachineRepository repository = new InMemoryMachineRepository();
        repository.saveMachine(new Machine("m-01", "area-cardio")).join();
        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        MachineSession started = service.startMachineSession(new StartMachineSessionMessage("m-01", "badge-01")).join();
        MachineSession ended = service.endMachineSession(new EndMachineSessionMessage("m-01")).join();

        assertEquals(started.getSessionId(), ended.getSessionId());
        assertNotNull(ended.getEndTime());

        Machine status = service.getMachineStatus("m-01").join();
        assertEquals(OccupancyStatus.FREE, status.getStatus());
        assertNull(status.getActiveSessionId());
    }

    @Test
    void setMaintenanceFailsWhenMachineOccupied() {
        InMemoryMachineRepository repository = new InMemoryMachineRepository();
        repository.saveMachine(new Machine("m-01", "area-cardio")).join();
        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        service.startMachineSession(new StartMachineSessionMessage("m-01", "badge-01")).join();

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> service.setMachineMaintenance(new SetMachineMaintenanceMessage("m-01")).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    void getMachineHistoryReturnsSessionsSortedByStartDesc() {
        InMemoryMachineRepository repository = new InMemoryMachineRepository();
        Machine machine = new Machine("m-01", "area-cardio");
        repository.saveMachine(machine).join();

        repository.saveMachineSession(new MachineSession(
                "session-old",
                "m-01",
                "badge-01",
                LocalDateTime.of(2026, 3, 27, 10, 0),
                LocalDateTime.of(2026, 3, 27, 10, 20)
        )).join();
        repository.saveMachineSession(new MachineSession(
                "session-new",
                "m-01",
                "badge-02",
                LocalDateTime.of(2026, 3, 27, 11, 0),
                LocalDateTime.of(2026, 3, 27, 11, 10)
        )).join();

        MachineServiceAPIImpl service = new MachineServiceAPIImpl(repository);

        List<MachineSession> history = service.getMachineHistory("m-01").join();

        assertEquals(2, history.size());
        assertEquals("session-new", history.get(0).getSessionId());
        assertEquals("session-old", history.get(1).getSessionId());
    }

    private static final class InMemoryMachineRepository implements MachineRepository {

        private final Map<String, Machine> machinesById = new LinkedHashMap<>();
        private final Map<String, MachineSession> sessionsById = new LinkedHashMap<>();

        @Override
        public CompletableFuture<Void> saveMachine(Machine machine) {
            machinesById.put(machine.getMachineId(), machine);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<Machine>> findMachineById(String machineId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(machinesById.get(machineId)));
        }

        @Override
        public CompletableFuture<Void> saveMachineSession(MachineSession session) {
            sessionsById.put(session.getSessionId(), session);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<MachineSession>> findActiveSessionByMachineId(String machineId) {
            return CompletableFuture.completedFuture(
                    sessionsById.values().stream()
                            .filter(s -> s.getMachineId().equals(machineId))
                            .filter(MachineSession::isActive)
                            .findFirst()
            );
        }

        @Override
        public CompletableFuture<List<MachineSession>> findMachineHistoryByMachineId(String machineId) {
            List<MachineSession> history = sessionsById.values().stream()
                    .filter(s -> s.getMachineId().equals(machineId))
                    .sorted(Comparator.comparing(MachineSession::getStartTime).reversed())
                    .collect(Collectors.toCollection(ArrayList::new));

            return CompletableFuture.completedFuture(history);
        }
    }
}