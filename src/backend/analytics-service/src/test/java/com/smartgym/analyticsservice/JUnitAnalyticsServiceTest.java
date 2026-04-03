package com.smartgym.analyticsservice;

import com.smartgym.analyticsservice.application.AnalyticsServiceAPIImpl;
import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaAttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaPeakHourStat;
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
    void ingestAreaAccessInUpdatesAreaAttendanceAndAreaPeakHours() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        JsonObject event = new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("timeStamp", "2026-03-26T10:15:00Z")
                        .put("areaId", "cardio")
                        .put("direction", "IN")
                );

        analyticsService.ingestEvent(event).join();

        Optional<AreaAttendanceSnapshot> snapshot = analyticsService
                .getAreaAttendanceByDateAndAreaId("2026-03-26", "cardio")
                .join();
        List<AreaPeakHourStat> peakHours = analyticsService
                .getAreaPeakHoursByDateAndAreaId("2026-03-26", "cardio")
                .join();

        assertTrue(snapshot.isPresent());
        assertEquals(1, snapshot.get().getCurrentCount());
        assertEquals(1, snapshot.get().getTotalEntries());
        assertEquals(0, snapshot.get().getTotalExits());
        assertEquals(1, peakHours.size());
        assertEquals(10, peakHours.getFirst().getHour());
        assertEquals(1, peakHours.getFirst().getAttendanceCount());
    }

    @Test
    void ingestAreaAccessOutNeverDropsAreaAttendanceBelowZero() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        JsonObject event = new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("timeStamp", "2026-03-26T10:15:00Z")
                        .put("areaId", "cardio")
                        .put("direction", "OUT")
                );

        analyticsService.ingestEvent(event).join();

        Optional<AreaAttendanceSnapshot> snapshot = analyticsService
                .getAreaAttendanceByDateAndAreaId("2026-03-26", "cardio")
                .join();

        assertTrue(snapshot.isPresent());
        assertEquals(0, snapshot.get().getCurrentCount());
        assertEquals(0, snapshot.get().getTotalEntries());
        assertEquals(1, snapshot.get().getTotalExits());
    }

    @Test
    void ingestAreaAccessFailsWhenDirectionIsInvalid() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        JsonObject invalidEvent = new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("timeStamp", "2026-03-26T10:15:00Z")
                        .put("areaId", "cardio")
                        .put("direction", "SIDEWAYS")
                );

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> analyticsService.ingestEvent(invalidEvent).join()
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("direction IN or OUT"));
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

    @Test
    void ingestGymAccessEntryExitSequenceKeepsAttendanceConsistentAndTracksEntryPeaksOnly() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "GYM_ACCESS")
                .put("payload", new JsonObject()
                        .put("accessType", "ENTRY")
                        .put("timeStamp", "2026-03-26T08:00:00Z"))
        ).join();

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "GYM_ACCESS")
                .put("payload", new JsonObject()
                        .put("accessType", "ENTRY")
                        .put("timeStamp", "2026-03-26T09:00:00Z"))
        ).join();

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "GYM_ACCESS")
                .put("payload", new JsonObject()
                        .put("accessType", "EXIT")
                        .put("timeStamp", "2026-03-26T09:30:00Z"))
        ).join();

        Optional<AttendanceSnapshot> snapshot = analyticsService.getAttendanceStats("2026-03-26").join();
        List<PeakHourStat> peakHours = analyticsService.getPeakHoursByDate("2026-03-26").join();

        assertTrue(snapshot.isPresent());
        assertEquals(2, snapshot.get().getTotalEntries());
        assertEquals(1, snapshot.get().getTotalExits());
        assertEquals(1, snapshot.get().getGymCount());

        Map<Integer, Integer> byHour = new LinkedHashMap<>();
        for (PeakHourStat stat : peakHours) {
            byHour.put(stat.getHour(), stat.getAttendanceCount());
        }

        assertEquals(2, byHour.size());
        assertEquals(1, byHour.get(8));
        assertEquals(1, byHour.get(9));
    }

    @Test
    void ingestAreaAccessSequenceTracksCurrentEntriesExitsAndHourPeakAsMaxOccupancy() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("areaId", "cardio")
                        .put("direction", "IN")
                        .put("timeStamp", "2026-03-26T10:00:00Z"))
        ).join();

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("areaId", "cardio")
                        .put("direction", "IN")
                        .put("timeStamp", "2026-03-26T10:10:00Z"))
        ).join();

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("areaId", "cardio")
                        .put("direction", "OUT")
                        .put("timeStamp", "2026-03-26T10:20:00Z"))
        ).join();

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("areaId", "cardio")
                        .put("direction", "IN")
                        .put("timeStamp", "2026-03-26T10:30:00Z"))
        ).join();

        Optional<AreaAttendanceSnapshot> snapshot = analyticsService
                .getAreaAttendanceByDateAndAreaId("2026-03-26", "cardio")
                .join();
        List<AreaPeakHourStat> peakHours = analyticsService
                .getAreaPeakHoursByDateAndAreaId("2026-03-26", "cardio")
                .join();

        assertTrue(snapshot.isPresent());
        assertEquals(2, snapshot.get().getCurrentCount());
        assertEquals(3, snapshot.get().getTotalEntries());
        assertEquals(1, snapshot.get().getTotalExits());
        assertEquals(1, peakHours.size());
        assertEquals(10, peakHours.getFirst().getHour());
        assertEquals(2, peakHours.getFirst().getAttendanceCount());
    }

    @Test
    void ingestMachineUsageCountsOnlyStartedAndKeepsDailyIsolation() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "MACHINE_USAGE")
                .put("payload", new JsonObject()
                        .put("machineId", "treadmill-01")
                        .put("timeStamp", "2026-03-26T11:00:00Z")
                        .put("usageState", "STARTED"))
        ).join();

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "MACHINE_USAGE")
                .put("payload", new JsonObject()
                        .put("machineId", "treadmill-01")
                        .put("timeStamp", "2026-03-26T11:30:00Z")
                        .put("usageState", "STOPPED"))
        ).join();

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "MACHINE_USAGE")
                .put("payload", new JsonObject()
                        .put("machineId", "treadmill-01")
                        .put("timeStamp", "2026-03-27T08:30:00Z")
                        .put("usageState", "STARTED"))
        ).join();

        List<MachineUtilization> day1 = analyticsService.getMachineUtilizationByDate("2026-03-26").join();
        List<MachineUtilization> day2 = analyticsService.getMachineUtilizationByDate("2026-03-27").join();

        assertEquals(1, day1.size());
        assertEquals("treadmill-01", day1.getFirst().getMachineId());
        assertEquals(1, day1.getFirst().getUsageCount());
        assertEquals(30.0, day1.getFirst().getTotalUsageMinutes(), 0.0001);
        assertEquals(50.0, day1.getFirst().getUtilizationRate(), 0.0001);

        assertEquals(1, day2.size());
        assertEquals("treadmill-01", day2.getFirst().getMachineId());
        assertEquals(1, day2.getFirst().getUsageCount());
        assertEquals(0.0, day2.getFirst().getTotalUsageMinutes(), 0.0001);
        assertEquals(0.0, day2.getFirst().getUtilizationRate(), 0.0001);
    }

    @Test
    void ingestMachineUsageStoppedWithoutStartedDoesNotIncreaseTotals() {
        AnalyticsRepository repository = new InMemoryAnalyticsRepository(
                Map.of(),
                List.of(),
                List.of()
        );

        AnalyticsServiceAPIImpl analyticsService = new AnalyticsServiceAPIImpl(repository);

        analyticsService.ingestEvent(new JsonObject()
                .put("eventType", "MACHINE_USAGE")
                .put("payload", new JsonObject()
                        .put("machineId", "bike-01")
                        .put("timeStamp", "2026-03-26T12:00:00Z")
                        .put("usageState", "STOPPED"))
        ).join();

        List<MachineUtilization> result = analyticsService.getMachineUtilizationByDate("2026-03-26").join();

        assertEquals(1, result.size());
        assertEquals("bike-01", result.getFirst().getMachineId());
        assertEquals(0, result.getFirst().getUsageCount());
        assertEquals(0.0, result.getFirst().getTotalUsageMinutes(), 0.0001);
        assertEquals(0.0, result.getFirst().getUtilizationRate(), 0.0001);
    }

    private static final class InMemoryAnalyticsRepository implements AnalyticsRepository {

        private final Map<String, AttendanceSnapshot> attendanceByDate;
        private final List<MachineUtilization> machineUtilizations;
        private final List<PeakHourStat> peakHourStats;
        private final Map<String, AreaAttendanceSnapshot> areaAttendanceByDateAndArea;
        private final List<AreaPeakHourStat> areaPeakHourStats;

        private InMemoryAnalyticsRepository(
                Map<String, AttendanceSnapshot> attendanceByDate,
                List<MachineUtilization> machineUtilizations,
                List<PeakHourStat> peakHourStats
        ) {
            this.attendanceByDate = new LinkedHashMap<>(attendanceByDate);
            this.machineUtilizations = new ArrayList<>(machineUtilizations);
            this.peakHourStats = new ArrayList<>(peakHourStats);
            this.areaAttendanceByDateAndArea = new LinkedHashMap<>();
            this.areaPeakHourStats = new ArrayList<>();
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
            int existingIndex = -1;
            for (int i = 0; i < machineUtilizations.size(); i++) {
                if (machineUtilizations.get(i).getId().equals(machineUtilization.getId())) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex >= 0) {
                machineUtilizations.set(existingIndex, machineUtilization);
            } else {
                machineUtilizations.add(machineUtilization);
            }
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
            int existingIndex = -1;
            for (int i = 0; i < peakHourStats.size(); i++) {
                if (peakHourStats.get(i).getId().equals(peakHourStat.getId())) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex >= 0) {
                peakHourStats.set(existingIndex, peakHourStat);
            } else {
                peakHourStats.add(peakHourStat);
            }
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

        @Override
        public CompletableFuture<Void> saveAreaAttendanceSnapshot(AreaAttendanceSnapshot snapshot) {
            areaAttendanceByDateAndArea.put(areaKey(snapshot.getDate(), snapshot.getAreaId()), snapshot);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<AreaAttendanceSnapshot>> findAreaAttendanceByDateAndAreaId(String date, String areaId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(areaAttendanceByDateAndArea.get(areaKey(date, areaId))));
        }

        @Override
        public CompletableFuture<List<AreaAttendanceSnapshot>> findAreaAttendanceByDate(String date) {
            List<AreaAttendanceSnapshot> result = new ArrayList<>();

            for (AreaAttendanceSnapshot snapshot : areaAttendanceByDateAndArea.values()) {
                if (date.equals(snapshot.getDate())) {
                    result.add(snapshot);
                }
            }

            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletableFuture<List<AreaAttendanceSnapshot>> findAllAreaAttendanceSnapshots() {
            return CompletableFuture.completedFuture(new ArrayList<>(areaAttendanceByDateAndArea.values()));
        }

        @Override
        public CompletableFuture<Void> saveAreaPeakHourStat(AreaPeakHourStat areaPeakHourStat) {
            int existingIndex = -1;
            for (int i = 0; i < areaPeakHourStats.size(); i++) {
                if (areaPeakHourStats.get(i).getId().equals(areaPeakHourStat.getId())) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex >= 0) {
                areaPeakHourStats.set(existingIndex, areaPeakHourStat);
            } else {
                areaPeakHourStats.add(areaPeakHourStat);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<List<AreaPeakHourStat>> findAreaPeakHoursByDate(String date) {
            List<AreaPeakHourStat> result = new ArrayList<>();

            for (AreaPeakHourStat areaPeakHourStat : areaPeakHourStats) {
                if (date.equals(areaPeakHourStat.getDate())) {
                    result.add(areaPeakHourStat);
                }
            }

            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletableFuture<List<AreaPeakHourStat>> findAreaPeakHoursByDateAndAreaId(String date, String areaId) {
            List<AreaPeakHourStat> result = new ArrayList<>();

            for (AreaPeakHourStat areaPeakHourStat : areaPeakHourStats) {
                if (date.equals(areaPeakHourStat.getDate()) && areaId.equals(areaPeakHourStat.getAreaId())) {
                    result.add(areaPeakHourStat);
                }
            }

            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletableFuture<List<AreaPeakHourStat>> findAllAreaPeakHours() {
            return CompletableFuture.completedFuture(new ArrayList<>(areaPeakHourStats));
        }

        private String areaKey(String date, String areaId) {
            return date + "::" + areaId;
        }
    }
}

