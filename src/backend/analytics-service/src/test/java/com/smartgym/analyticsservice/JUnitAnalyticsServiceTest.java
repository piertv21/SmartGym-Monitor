package com.smartgym.analyticsservice;

import com.smartgym.analyticsservice.application.AnalyticsServiceAPIImpl;
import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

public class JUnitAnalyticsServiceTest {

    @Test
    void ingestGymAccessEntryUpdatesAttendanceAndPeakHours() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        JsonObject event = new JsonObject()
                .put("eventType", "GYM_ACCESS")
                .put("payload", new JsonObject()
                        .put("timeStamp", "2026-03-26T18:30:00Z")
                        .put("accessType", "ENTRY")
                );

        analyticsService.ingestEvent(event).join();

        Optional<AttendanceSnapshot> snapshot = analyticsService.getAttendanceStats("2026-03-26").join();
        List<PeakHourStat> peakHours = analyticsService.getPeakHoursByDate("2026-03-26").join();

        assertTrue(snapshot.isPresent());
        assertEquals(1, snapshot.get().getTotalEntries());
        assertEquals(0, snapshot.get().getTotalExits());
        assertEquals(1, snapshot.get().getGymCount());
        assertEquals(1, peakHours.size());
        assertEquals(18, peakHours.getFirst().getHour());
        assertEquals(1, peakHours.getFirst().getAttendanceCount());
    }

    @Test
    void ingestMachineUsageStartedUpdatesMachineUtilization() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        JsonObject event = new JsonObject()
                .put("eventType", "MACHINE_USAGE")
                .put("payload", new JsonObject()
                        .put("timeStamp", "2026-03-26T10:15:00Z")
                        .put("machineId", "treadmill-01")
                        .put("usageState", "STARTED")
                );

        analyticsService.ingestEvent(event).join();

        List<MachineUtilization> util = analyticsService.getMachineUtilizationByDate("2026-03-26").join();
        assertEquals(1, util.size());
        assertEquals("treadmill-01", util.getFirst().getMachineId());
        assertEquals(1, util.getFirst().getUsageCount());
    }

    @Test
    void ingestGymAccessAcceptsTimestampAlias() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        JsonObject event = new JsonObject()
                .put("eventType", "GYM_ACCESS")
                .put("payload", new JsonObject()
                        .put("timestamp", "2026-03-26T12:00:00Z")
                        .put("accessType", "ENTRY")
                );

        analyticsService.ingestEvent(event).join();

        Optional<AttendanceSnapshot> snapshot = analyticsService.getAttendanceStats("2026-03-26").join();
        assertTrue(snapshot.isPresent());
        assertEquals(1, snapshot.get().getTotalEntries());
    }

    @Test
    void ingestEventFailsWhenPayloadIsMissing() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        JsonObject invalidEvent = new JsonObject().put("eventType", "GYM_ACCESS");

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> analyticsService.ingestEvent(invalidEvent).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void getAttendanceStatsReturnsSnapshotWhenPresent() {
        AttendanceSnapshot snapshot = new AttendanceSnapshot(
                "att-001",
                "2026-03-26",
                12,
                30,
                18
        );

        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of("2026-03-26", snapshot),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        Optional<AttendanceSnapshot> result = analyticsService.getAttendanceStats("2026-03-26").join();

        assertTrue(result.isPresent());
        assertEquals("2026-03-26", result.get().getDate());
        assertEquals(12, result.get().getGymCount());
    }

    @Test
    void getAttendanceStatsReturnsEmptyWhenMissing() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        Optional<AttendanceSnapshot> result = analyticsService.getAttendanceStats("2026-03-26").join();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAttendanceStatsFailsWhenDateIsBlank() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> analyticsService.getAttendanceStats(" ").join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("date cannot be null or empty"));
    }

    @Test
    void getAllAttendanceStatsReturnsAllSnapshots() {
        AttendanceSnapshot snapshot1 = new AttendanceSnapshot("att-001", "2026-03-25", 10, 20, 10);
        AttendanceSnapshot snapshot2 = new AttendanceSnapshot("att-002", "2026-03-26", 15, 35, 20);

        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(
                        "2026-03-25", snapshot1,
                        "2026-03-26", snapshot2
                ),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        List<AttendanceSnapshot> result = analyticsService.getAllAttendanceStats().join();

        assertEquals(2, result.size());
    }

    @Test
    void getMachineUtilizationReturnsAllMachineStats() {
        MachineUtilization machine1 = new MachineUtilization(
                "mu-001",
                "treadmill-01",
                "2026-03-26",
                5,
                120.0,
                65.5
        );

        MachineUtilization machine2 = new MachineUtilization(
                "mu-002",
                "bench-press-01",
                "2026-03-26",
                3,
                80.0,
                43.0
        );

        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(machine1, machine2),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        List<MachineUtilization> result = analyticsService.getMachineUtilization().join();

        assertEquals(2, result.size());
    }

    @Test
    void getMachineUtilizationByDateReturnsFilteredStats() {
        MachineUtilization machine1 = new MachineUtilization(
                "mu-001",
                "treadmill-01",
                "2026-03-26",
                5,
                120.0,
                65.5
        );

        MachineUtilization machine2 = new MachineUtilization(
                "mu-002",
                "bench-press-01",
                "2026-03-25",
                3,
                80.0,
                43.0
        );

        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(machine1, machine2),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        List<MachineUtilization> result = analyticsService.getMachineUtilizationByDate("2026-03-26").join();

        assertEquals(1, result.size());
        assertEquals("treadmill-01", result.getFirst().getMachineId());
    }

    @Test
    void getMachineUtilizationByDateFailsWhenDateIsBlank() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> analyticsService.getMachineUtilizationByDate("").join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("date cannot be null or empty"));
    }

    @Test
    void getPeakHoursReturnsAllPeakHours() {
        PeakHourStat stat1 = new PeakHourStat("ph-001", "2026-03-26", 18, 25);
        PeakHourStat stat2 = new PeakHourStat("ph-002", "2026-03-26", 19, 28);

        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of(stat1, stat2)
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        List<PeakHourStat> result = analyticsService.getPeakHours().join();

        assertEquals(2, result.size());
    }

    @Test
    void getPeakHoursByDateReturnsFilteredPeakHours() {
        PeakHourStat stat1 = new PeakHourStat("ph-001", "2026-03-26", 18, 25);
        PeakHourStat stat2 = new PeakHourStat("ph-002", "2026-03-25", 19, 28);

        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of(stat1, stat2)
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        List<PeakHourStat> result = analyticsService.getPeakHoursByDate("2026-03-26").join();

        assertEquals(1, result.size());
        assertEquals(18, result.getFirst().getHour());
    }

    @Test
    void getPeakHoursByDateFailsWhenDateIsBlank() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> analyticsService.getPeakHoursByDate(" ").join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("date cannot be null or empty"));
    }

    private static final class InMemoryAnalyticsRepository implements AnalyticsRepository {

        private final Map<String, AttendanceSnapshot> attendanceByDate;
        private final List<MachineUtilization> machineUtilizations;
        private final List<PeakHourStat> peakHourStats;

        private InMemoryAnalyticsRepository(
                Map<String, AttendanceSnapshot> attendanceByDate,
                List<MachineUtilization> machineUtilizations,
                List<PeakHourStat> peakHourStats
        ) {
            this.attendanceByDate = new LinkedHashMap<>(attendanceByDate);
            this.machineUtilizations = new ArrayList<>(machineUtilizations);
            this.peakHourStats = new ArrayList<>(peakHourStats);
        }

        @Override
        public CompletableFuture<Void> saveAttendanceSnapshot(AttendanceSnapshot snapshot) {
            attendanceByDate.put(snapshot.getDate(), snapshot);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<AttendanceSnapshot>> findAttendanceByDate(String date) {
            return CompletableFuture.completedFuture(Optional.ofNullable(attendanceByDate.get(date)));
        }

        @Override
        public CompletableFuture<List<AttendanceSnapshot>> findAllAttendanceSnapshots() {
            return CompletableFuture.completedFuture(new ArrayList<>(attendanceByDate.values()));
        }

        @Override
        public CompletableFuture<Void> saveMachineUtilization(MachineUtilization machineUtilization) {
            machineUtilizations.add(machineUtilization);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<List<MachineUtilization>> findAllMachineUtilizations() {
            return CompletableFuture.completedFuture(new ArrayList<>(machineUtilizations));
        }

        @Override
        public CompletableFuture<List<MachineUtilization>> findMachineUtilizationsByDate(String date) {
            List<MachineUtilization> result = new ArrayList<>();

            for (MachineUtilization machineUtilization : machineUtilizations) {
                if (date.equals(machineUtilization.getDate())) {
                    result.add(machineUtilization);
                }
            }

            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletableFuture<Void> savePeakHourStat(PeakHourStat peakHourStat) {
            peakHourStats.add(peakHourStat);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<List<PeakHourStat>> findPeakHoursByDate(String date) {
            List<PeakHourStat> result = new ArrayList<>();

            for (PeakHourStat peakHourStat : peakHourStats) {
                if (date.equals(peakHourStat.getDate())) {
                    result.add(peakHourStat);
                }
            }

            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletableFuture<List<PeakHourStat>> findAllPeakHours() {
            return CompletableFuture.completedFuture(new ArrayList<>(peakHourStats));
        }
    }
}