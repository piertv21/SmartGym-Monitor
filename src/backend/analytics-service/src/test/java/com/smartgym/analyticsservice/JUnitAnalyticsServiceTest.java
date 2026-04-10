package com.smartgym.analyticsservice;

import com.smartgym.analyticsservice.application.AnalyticsServiceAPIImpl;
import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.AttendanceSeriesResponse;
import com.smartgym.analyticsservice.model.AreaAttendanceSnapshot;
import com.smartgym.analyticsservice.model.AreaSessionDurationStat;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.GymSessionDurationStat;
import com.smartgym.analyticsservice.model.PeakHourStat;
import com.smartgym.analyticsservice.model.UniqueUsersStat;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class JUnitAnalyticsServiceTest {

    private static final ZoneId ANALYTICS_ZONE = ZoneId.of("Europe/Rome");

    @Test
    void ingestAndReadAttendanceByDate() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T09:00:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T09:30:00Z", "badge-02")).join();
        service.ingestEvent(gymEvent("EXIT", "2026-03-26T10:00:00Z", "badge-01")).join();

        Optional<AttendanceSnapshot> stats = service.getAttendanceStats("2026-03-26").join();
        assertTrue(stats.isPresent());
        assertEquals(2, stats.get().getTotalEntries());
        assertEquals(1, stats.get().getTotalExits());
        assertEquals(1, stats.get().getGymCount());
    }

    @Test
    void machineUtilizationByDateAndMonth() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(machineEvent("STARTED", "2026-03-26T08:00:00Z", "bike-01")).join();
        service.ingestEvent(machineEvent("STOPPED", "2026-03-26T08:20:00Z", "bike-01")).join();
        service.ingestEvent(machineEvent("STARTED", "2026-03-27T08:00:00Z", "bike-01")).join();
        service.ingestEvent(machineEvent("STOPPED", "2026-03-27T08:10:00Z", "bike-01")).join();

        List<MachineUtilization> day = service.getMachineUtilizationByDate("2026-03-26").join();
        List<MachineUtilization> month = service.getMachineUtilizationByMonth("2026-03").join();

        assertEquals(1, day.size());
        assertEquals(1, day.getFirst().getUsageCount());
        assertEquals(20.0, day.getFirst().getTotalUsageMinutes(), 0.0001);

        assertEquals(1, month.size());
        assertEquals(2, month.getFirst().getUsageCount());
        assertEquals(30.0, month.getFirst().getTotalUsageMinutes(), 0.0001);
    }

    @Test
    void uniqueUsersAndGymDurationStats() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T09:00:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("EXIT", "2026-03-26T09:30:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T10:00:00Z", "badge-02")).join();
        service.ingestEvent(gymEvent("EXIT", "2026-03-26T10:20:00Z", "badge-02")).join();

        UniqueUsersStat unique = service.getUniqueUsersByDate("2026-03-26").join();
        GymSessionDurationStat duration = service.getGymSessionDurationByDate("2026-03-26").join();

        assertEquals(2, unique.getUniqueUsers());
        assertEquals(25.0, duration.getAverageDurationMinutes(), 0.0001);
        assertEquals(30.0, duration.getMaxDurationMinutes(), 0.0001);
        assertEquals(2, duration.getSessionCount());
    }

    @Test
    void areaDurationAndAttendanceStats() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(areaEvent("IN", "2026-03-26T11:00:00Z", "badge-10", "cardio")).join();
        service.ingestEvent(areaEvent("OUT", "2026-03-26T11:40:00Z", "badge-10", "cardio")).join();
        service.ingestEvent(areaEvent("IN", "2026-03-26T12:00:00Z", "badge-11", "cardio")).join();

        AreaSessionDurationStat duration = service.getAreaSessionDurationByDate("2026-03-26", "cardio").join();
        Optional<AreaAttendanceSnapshot> attendance = service
                .getAreaAttendanceByDateAndAreaId("2026-03-26", "cardio")
                .join();

        assertEquals(40.0, duration.getAverageDurationMinutes(), 0.0001);
        assertEquals(40.0, duration.getMaxDurationMinutes(), 0.0001);
        assertEquals(1, duration.getSessionCount());
        assertTrue(attendance.isPresent());
        assertEquals(2, attendance.get().getTotalEntries());
        assertEquals(1, attendance.get().getTotalExits());
        assertEquals(1, attendance.get().getCurrentCount());
    }

    @Test
    void invalidPayloadFailsFast() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.ingestEvent(new JsonObject()
                        .put("eventType", "MACHINE_USAGE")
                        .put("payload", new JsonObject()
                                .put("timeStamp", "2026-03-26T10:00:00Z")
                                .put("usageState", "STARTED"))
                        )
        );
        assertTrue(ex.getMessage().contains("machineId"));
    }

    @Test
    void ingestSupportsTimestampAlias() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(new JsonObject()
                .put("eventType", "GYM_ACCESS")
                .put("payload", new JsonObject()
                        .put("timestamp", "2026-03-26T09:00:00Z")
                        .put("accessType", "ENTRY")
                        .put("badgeId", "badge-01"))
        ).join();

        Optional<AttendanceSnapshot> stats = service.getAttendanceStats("2026-03-26").join();
        assertTrue(stats.isPresent());
        assertEquals(1, stats.get().getTotalEntries());
    }

    @Test
    void unsupportedEventTypeFailsFast() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.ingestEvent(new JsonObject()
                        .put("eventType", "DEVICE_STATUS")
                        .put("payload", new JsonObject().put("timeStamp", "2026-03-26T10:00:00Z")))
        );

        assertTrue(ex.getMessage().contains("Unsupported eventType"));
    }

    @Test
    void getAttendanceStatsReturnsEmptyWhenNoEventsExistForDate() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        Optional<AttendanceSnapshot> stats = service.getAttendanceStats("2026-03-28").join();
        assertTrue(stats.isEmpty());
    }

    @Test
    void areaDurationByMonthAggregatesAllMatchingSessions() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(areaEvent("IN", "2026-03-01T10:00:00Z", "badge-01", "weights")).join();
        service.ingestEvent(areaEvent("OUT", "2026-03-01T10:30:00Z", "badge-01", "weights")).join();
        service.ingestEvent(areaEvent("IN", "2026-03-15T11:00:00Z", "badge-02", "weights")).join();
        service.ingestEvent(areaEvent("OUT", "2026-03-15T11:45:00Z", "badge-02", "weights")).join();
        service.ingestEvent(areaEvent("IN", "2026-04-01T09:00:00Z", "badge-03", "weights")).join();
        service.ingestEvent(areaEvent("OUT", "2026-04-01T09:20:00Z", "badge-03", "weights")).join();

        AreaSessionDurationStat month = service.getAreaSessionDurationByMonth("2026-03", "weights").join();

        assertEquals("2026-03", month.getPeriodValue());
        assertEquals(2, month.getSessionCount());
        assertEquals(37.5, month.getAverageDurationMinutes(), 0.0001);
        assertEquals(45.0, month.getMaxDurationMinutes(), 0.0001);
    }

    @Test
    void gymDurationIgnoresDanglingEntrySessions() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T08:00:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("EXIT", "2026-03-26T08:30:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T09:00:00Z", "badge-02")).join();

        GymSessionDurationStat stat = service.getGymSessionDurationByDate("2026-03-26").join();

        assertEquals(1, stat.getSessionCount());
        assertEquals(30.0, stat.getAverageDurationMinutes(), 0.0001);
        assertEquals(30.0, stat.getMaxDurationMinutes(), 0.0001);
    }

    @Test
    void areaDurationIgnoresUnmatchedOutAndOpenIn() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(areaEvent("OUT", "2026-03-26T09:00:00Z", "badge-01", "cardio")).join();
        service.ingestEvent(areaEvent("IN", "2026-03-26T10:00:00Z", "badge-01", "cardio")).join();
        service.ingestEvent(areaEvent("OUT", "2026-03-26T10:25:00Z", "badge-01", "cardio")).join();
        service.ingestEvent(areaEvent("IN", "2026-03-26T11:00:00Z", "badge-02", "cardio")).join();

        AreaSessionDurationStat stat = service.getAreaSessionDurationByDate("2026-03-26", "cardio").join();

        assertEquals(1, stat.getSessionCount());
        assertEquals(25.0, stat.getAverageDurationMinutes(), 0.0001);
        assertEquals(25.0, stat.getMaxDurationMinutes(), 0.0001);
    }

    @Test
    void gymDurationIsDeterministicWithOutOfOrderEvents() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(gymEvent("EXIT", "2026-03-26T09:30:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T09:00:00Z", "badge-01")).join();

        GymSessionDurationStat stat = service.getGymSessionDurationByDate("2026-03-26").join();

        assertEquals(1, stat.getSessionCount());
        assertEquals(30.0, stat.getAverageDurationMinutes(), 0.0001);
        assertEquals(30.0, stat.getMaxDurationMinutes(), 0.0001);
    }

    @Test
    void machineUtilizationListIsSortedByMachineId() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(machineEvent("STARTED", "2026-03-26T08:00:00Z", "z-bike")).join();
        service.ingestEvent(machineEvent("STOPPED", "2026-03-26T08:10:00Z", "z-bike")).join();
        service.ingestEvent(machineEvent("STARTED", "2026-03-26T08:05:00Z", "a-bike")).join();
        service.ingestEvent(machineEvent("STOPPED", "2026-03-26T08:20:00Z", "a-bike")).join();

        List<MachineUtilization> stats = service.getMachineUtilizationByDate("2026-03-26").join();

        assertEquals(2, stats.size());
        assertEquals("a-bike", stats.get(0).getMachineId());
        assertEquals("z-bike", stats.get(1).getMachineId());
    }

    @Test
    void peakHoursUsesEuropeRomeTimezone() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(gymEvent("ENTRY", "2026-03-26T08:00:00Z", "badge-01")).join();

        List<PeakHourStat> stats = service.getPeakHoursByDate("2026-03-26").join();

        assertEquals(1, stats.size());
        assertEquals(9, stats.getFirst().getHour());
    }

    @Test
    void attendanceSeriesDailyGlobalIncludesZeroBuckets() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(gymEvent("ENTRY", "2026-04-01T09:00:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("EXIT", "2026-04-03T10:00:00Z", "badge-01")).join();

        AttendanceSeriesResponse response = service
                .getAttendanceSeries("2026-04-01", "2026-04-03", "daily", null)
                .join();

        assertEquals("global", response.getMeta().getScope());
        assertEquals("daily", response.getMeta().getGranularity());
        assertEquals(3, response.getSeries().size());
        assertEquals(1, response.getSeries().get(0).getCurrentCount());
        assertEquals("2026-04-02", response.getSeries().get(1).getPeriod());
        assertEquals(0, response.getSeries().get(1).getCurrentCount());
        assertEquals(0, response.getSeries().get(1).getTotalEntries());
        assertEquals(0, response.getSeries().get(1).getTotalExits());
    }

    @Test
    void attendanceSeriesMonthlyAreaFiltersByAreaId() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(areaEvent("IN", "2026-03-01T10:00:00Z", "badge-01", "cardio")).join();
        service.ingestEvent(areaEvent("OUT", "2026-03-01T10:30:00Z", "badge-01", "cardio")).join();
        service.ingestEvent(areaEvent("IN", "2026-03-15T11:00:00Z", "badge-02", "weights")).join();
        service.ingestEvent(areaEvent("IN", "2026-04-01T12:00:00Z", "badge-03", "cardio")).join();

        AttendanceSeriesResponse response = service
                .getAttendanceSeries("2026-03-01", "2026-04-30", "monthly", "cardio")
                .join();

        assertEquals("area", response.getMeta().getScope());
        assertEquals("monthly", response.getMeta().getGranularity());
        assertEquals("cardio", response.getFilters().getAreaId());
        assertEquals(2, response.getSeries().size());
        assertEquals("2026-03", response.getSeries().get(0).getPeriod());
        assertEquals(1, response.getSeries().get(0).getCurrentCount());
        assertEquals(1, response.getSeries().get(0).getTotalEntries());
        assertEquals(1, response.getSeries().get(0).getTotalExits());
        assertEquals("2026-04", response.getSeries().get(1).getPeriod());
        assertEquals(1, response.getSeries().get(1).getCurrentCount());
        assertEquals(1, response.getSeries().get(1).getTotalEntries());
    }

    @Test
    void attendanceSeriesCurrentCountTracksEnteredUsersNotNetPresence() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        service.ingestEvent(gymEvent("ENTRY", "2026-04-10T09:00:00Z", "badge-01")).join();
        service.ingestEvent(gymEvent("EXIT", "2026-04-10T10:00:00Z", "badge-01")).join();

        AttendanceSeriesResponse response = service
                .getAttendanceSeries("2026-04-10", "2026-04-10", "daily", null)
                .join();

        assertEquals(1, response.getSeries().size());
        assertEquals(1, response.getSeries().getFirst().getCurrentCount());
        assertEquals(1, response.getSeries().getFirst().getTotalEntries());
        assertEquals(1, response.getSeries().getFirst().getTotalExits());
    }

    @Test
    void attendanceSeriesRejectsInvalidRange() {
        AnalyticsServiceAPIImpl service = new AnalyticsServiceAPIImpl(new InMemoryAnalyticsRepository());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.getAttendanceSeries("2026-04-10", "2026-04-01", "daily", null)
        );
        assertTrue(ex.getMessage().contains("from cannot be after to"));
    }

    private JsonObject gymEvent(String accessType, String ts, String badgeId) {
        return new JsonObject()
                .put("eventType", "GYM_ACCESS")
                .put("payload", new JsonObject()
                        .put("accessType", accessType)
                        .put("timeStamp", ts)
                        .put("badgeId", badgeId));
    }

    private JsonObject machineEvent(String state, String ts, String machineId) {
        return new JsonObject()
                .put("eventType", "MACHINE_USAGE")
                .put("payload", new JsonObject()
                        .put("usageState", state)
                        .put("timeStamp", ts)
                        .put("machineId", machineId));
    }

    private JsonObject areaEvent(String direction, String ts, String badgeId, String areaId) {
        return new JsonObject()
                .put("eventType", "AREA_ACCESS")
                .put("payload", new JsonObject()
                        .put("direction", direction)
                        .put("timeStamp", ts)
                        .put("badgeId", badgeId)
                        .put("areaId", areaId));
    }

    private static final class InMemoryAnalyticsRepository implements AnalyticsRepository {
        private final List<JsonObject> events = new ArrayList<>();

        @Override
        public CompletableFuture<Void> saveEvent(JsonObject event) {
            JsonObject payload = event.getJsonObject("payload", new JsonObject());
            String timestamp = payload.getString("timeStamp", payload.getString("timestamp"));
            LocalDate date = Instant.parse(timestamp).atZone(ANALYTICS_ZONE).toLocalDate();

            events.add(new JsonObject()
                    .put("eventType", event.getString("eventType"))
                    .put("eventDate", date.toString())
                    .put("eventMonth", YearMonth.from(date).toString())
                    .put("timestamp", timestamp)
                    .put("payload", payload.copy()));

            return CompletableFuture.completedFuture(null);
        }


        @Override
        public CompletableFuture<List<JsonObject>> findEventsByType(String eventType) {
            return CompletableFuture.completedFuture(events.stream()
                    .filter(event -> eventType.equals(event.getString("eventType")))
                    .map(JsonObject::copy)
                    .toList());
        }

        @Override
        public CompletableFuture<List<JsonObject>> findEventsByTypeAndDate(String eventType, String date) {
            return CompletableFuture.completedFuture(events.stream()
                    .filter(event -> eventType.equals(event.getString("eventType")) && date.equals(event.getString("eventDate")))
                    .map(JsonObject::copy)
                    .toList());
        }

        @Override
        public CompletableFuture<List<JsonObject>> findEventsByTypeAndDateRange(String eventType, String from, String to) {
            return CompletableFuture.completedFuture(events.stream()
                    .filter(event -> eventType.equals(event.getString("eventType")))
                    .filter(event -> {
                        String eventDate = event.getString("eventDate");
                        return eventDate.compareTo(from) >= 0 && eventDate.compareTo(to) <= 0;
                    })
                    .map(JsonObject::copy)
                    .toList());
        }

        @Override
        public CompletableFuture<List<JsonObject>> findEventsByTypeAndMonth(String eventType, String month) {
            return CompletableFuture.completedFuture(events.stream()
                    .filter(event -> eventType.equals(event.getString("eventType")) && month.equals(event.getString("eventMonth")))
                    .map(JsonObject::copy)
                    .toList());
        }
    }
}

