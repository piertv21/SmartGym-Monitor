package com.smartgym.machineservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smartgym.machineservice.application.ports.MachineRepository;
import com.smartgym.machineservice.model.Machine;
import com.smartgym.machineservice.model.MachineSession;
import com.smartgym.machineservice.model.OccupancyStatus;
import com.smartgym.machineservice.model.Sensor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MachineRepositoryImpl implements MachineRepository {

    private static final Logger logger = LoggerFactory.getLogger(MachineRepositoryImpl.class);
    private static final String DATABASE = "machineservicedb";
    private static final String MACHINES_COLLECTION = "machines";
    private static final String MACHINE_SESSIONS_COLLECTION = "machine_sessions";

    private final MongoCollection<Document> machinesCollection;
    private final MongoCollection<Document> machineSessionsCollection;

    public MachineRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase(DATABASE);
        this.machinesCollection = database.getCollection(MACHINES_COLLECTION);
        this.machineSessionsCollection = database.getCollection(MACHINE_SESSIONS_COLLECTION);
        logger.info(
                "MachineRepository initialized on database={} collections=[{}, {}]",
                DATABASE,
                MACHINES_COLLECTION,
                MACHINE_SESSIONS_COLLECTION);
    }

    @Override
    public CompletableFuture<List<Machine>> findAllMachines() {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<Machine> machines = new ArrayList<>();
                    for (Document document : machinesCollection.find()) {
                        machines.add(fromMachineDocument(document));
                    }
                    return machines;
                });
    }

    @Override
    public CompletableFuture<Void> saveMachine(Machine machine) {
        return CompletableFuture.runAsync(
                () -> {
                    Document filter = new Document("machineId", machine.getMachineId());
                    Document document = toMachineDocument(machine);

                    Document existing = machinesCollection.find(filter).first();
                    if (existing == null) {
                        machinesCollection.insertOne(document);
                    } else {
                        machinesCollection.replaceOne(filter, document);
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<Machine>> findMachineById(String machineId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    Document document =
                            machinesCollection.find(new Document("machineId", machineId)).first();
                    if (document == null) {
                        return Optional.empty();
                    }
                    return Optional.of(fromMachineDocument(document));
                });
    }

    @Override
    public CompletableFuture<Void> saveMachineSession(MachineSession session) {
        return CompletableFuture.runAsync(
                () -> {
                    Document filter = new Document("sessionId", session.getSessionId());
                    Document document = toSessionDocument(session);

                    Document existing = machineSessionsCollection.find(filter).first();
                    if (existing == null) {
                        machineSessionsCollection.insertOne(document);
                    } else {
                        machineSessionsCollection.replaceOne(filter, document);
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<MachineSession>> findActiveSessionByMachineId(
            String machineId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    Document filter = new Document("machineId", machineId).append("endTime", null);

                    Document document = machineSessionsCollection.find(filter).first();
                    if (document == null) {
                        return Optional.empty();
                    }
                    return Optional.of(fromSessionDocument(document));
                });
    }

    @Override
    public CompletableFuture<List<MachineSession>> findMachineHistoryByMachineId(String machineId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<MachineSession> result = new ArrayList<>();
                    for (Document document :
                            machineSessionsCollection
                                    .find(new Document("machineId", machineId))
                                    .sort(Sorts.descending("startTime"))) {
                        result.add(fromSessionDocument(document));
                    }
                    return result;
                });
    }

    @Override
    public CompletableFuture<List<MachineSession>> findMachineSessionsByStartTimeRange(
            String fromInclusive, String toExclusive) {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<MachineSession> result = new ArrayList<>();
                    for (Document document :
                            machineSessionsCollection
                                    .find(
                                            Filters.and(
                                                    Filters.gte("startTime", fromInclusive),
                                                    Filters.lt("startTime", toExclusive)))
                                    .sort(Sorts.ascending("startTime"))) {
                        result.add(fromSessionDocument(document));
                    }
                    return result;
                });
    }

    private Document toMachineDocument(Machine machine) {
        return new Document()
                .append("machineId", machine.getMachineId())
                .append("areaId", machine.getAreaId())
                .append("status", machine.getStatus().name())
                .append("activeSessionId", machine.getActiveSessionId())
                .append("sensor", machine.getSensor() == null ? null : machine.getSensor().getId());
    }

    private Machine fromMachineDocument(Document document) {
        String sensorId = document.getString("sensor");
        return new Machine(
                document.getString("machineId"),
                document.getString("areaId"),
                OccupancyStatus.valueOf(document.getString("status")),
                document.getString("activeSessionId"),
                sensorId == null ? null : new Sensor(sensorId));
    }

    private Document toSessionDocument(MachineSession session) {
        return new Document()
                .append("sessionId", session.getSessionId())
                .append("machineId", session.getMachineId())
                .append("badgeId", session.getBadgeId())
                .append("startTime", session.getStartTime().toString())
                .append(
                        "endTime",
                        session.getEndTime() == null ? null : session.getEndTime().toString());
    }

    private MachineSession fromSessionDocument(Document document) {
        String endTime = document.getString("endTime");
        return new MachineSession(
                document.getString("sessionId"),
                document.getString("machineId"),
                document.getString("badgeId"),
                LocalDateTime.parse(document.getString("startTime")),
                endTime == null ? null : LocalDateTime.parse(endTime));
    }
}
