package com.smartgym.machineservice.application;

import com.smartgym.machineservice.application.ports.MachineRepository;
import com.smartgym.machineservice.application.ports.MachineServiceAPI;
import com.smartgym.machineservice.model.ConfigureMachineMessage;
import com.smartgym.machineservice.model.EndMachineSessionMessage;
import com.smartgym.machineservice.model.Machine;
import com.smartgym.machineservice.model.MachineSession;
import com.smartgym.machineservice.model.MachineUsageSeriesResponse;
import com.smartgym.machineservice.model.OccupancyStatus;
import com.smartgym.machineservice.model.Sensor;
import com.smartgym.machineservice.model.SetMachineMaintenanceMessage;
import com.smartgym.machineservice.model.StartMachineSessionMessage;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    public CompletableFuture<MachineUsageSeriesResponse> getMachineUsageSeries(String from, String to, String granularity, String areaId) {
        LocalDate fromDate = parseLocalDate(from, "from");
        LocalDate toDate = parseLocalDate(to, "to");
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("from cannot be after to");
        }

        String normalizedGranularity = normalizeGranularity(granularity);
        String normalizedAreaId = normalizeOptionalArea(areaId);

        String fromInclusive = fromDate.atStartOfDay().toString();
        String toExclusive = toDate.plusDays(1).atStartOfDay().toString();

        CompletableFuture<List<MachineSession>> sessionsFuture = repository.findMachineSessionsByStartTimeRange(fromInclusive, toExclusive);
        CompletableFuture<List<Machine>> machinesFuture = repository.findAllMachines();

        return sessionsFuture.thenCombine(machinesFuture, (sessions, machines) -> {
            Map<String, String> areaByMachineId = machines.stream()
                    .collect(LinkedHashMap::new, (map, machine) -> map.put(machine.getMachineId(), machine.getAreaId()), Map::putAll);

            Map<String, List<MachineUsageSeriesResponse.SessionItem>> buckets = initializeBuckets(fromDate, toDate, normalizedGranularity);

            for (MachineSession session : sessions) {
                if (session.getEndTime() == null) {
                    continue;
                }

                String machineAreaId = areaByMachineId.get(session.getMachineId());
                if (normalizedAreaId != null && !normalizedAreaId.equals(machineAreaId)) {
                    continue;
                }

                String period = toPeriodKey(session.getStartTime().toLocalDate(), normalizedGranularity);
                List<MachineUsageSeriesResponse.SessionItem> periodSessions = buckets.get(period);
                if (periodSessions == null) {
                    continue;
                }

                periodSessions.add(toSessionItem(session, machineAreaId));
            }

            List<MachineUsageSeriesResponse.Point> series = buckets.entrySet().stream()
                    .map(entry -> new MachineUsageSeriesResponse.Point(entry.getKey(), entry.getValue()))
                    .toList();

            return new MachineUsageSeriesResponse(
                    new MachineUsageSeriesResponse.Meta(normalizedGranularity),
                    new MachineUsageSeriesResponse.Filters(fromDate.toString(), toDate.toString(), normalizedAreaId),
                    series
            );
        });
    }

    private Machine getRequiredMachine(String machineId) {
        return repository.findMachineById(machineId).join()
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + machineId));
    }

    private Map<String, List<MachineUsageSeriesResponse.SessionItem>> initializeBuckets(LocalDate from, LocalDate to, String granularity) {
        Map<String, List<MachineUsageSeriesResponse.SessionItem>> buckets = new LinkedHashMap<>();
        if ("monthly".equals(granularity)) {
            YearMonth cursor = YearMonth.from(from);
            YearMonth end = YearMonth.from(to);
            while (!cursor.isAfter(end)) {
                buckets.put(cursor.toString(), new ArrayList<>());
                cursor = cursor.plusMonths(1);
            }
            return buckets;
        }

        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            buckets.put(cursor.toString(), new ArrayList<>());
            cursor = cursor.plusDays(1);
        }
        return buckets;
    }

    private String toPeriodKey(LocalDate date, String granularity) {
        if ("monthly".equals(granularity)) {
            return YearMonth.from(date).toString();
        }
        return date.toString();
    }

    private MachineUsageSeriesResponse.SessionItem toSessionItem(MachineSession session, String areaId) {
        long durationSeconds = Duration.between(session.getStartTime(), session.getEndTime()).getSeconds();
        return new MachineUsageSeriesResponse.SessionItem(
                session.getMachineId(),
                areaId,
                session.getStartTime().toString(),
                session.getEndTime().toString(),
                durationSeconds,
                session.getBadgeId()
        );
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

    private LocalDate parseLocalDate(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(fieldName + " must be in format yyyy-MM-dd", ex);
        }
    }

    private String normalizeGranularity(String granularity) {
        if (isBlank(granularity)) {
            return "daily";
        }
        String normalized = granularity.trim().toLowerCase();
        if (!"daily".equals(normalized) && !"monthly".equals(normalized)) {
            throw new IllegalArgumentException("granularity must be daily or monthly");
        }
        return normalized;
    }

    private String normalizeOptionalArea(String areaId) {
        if (areaId == null) {
            return null;
        }
        String normalized = areaId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }

}
