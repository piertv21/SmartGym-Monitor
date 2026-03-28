package com.smartgym.analyticsservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import com.smartgym.analyticsservice.model.AttendanceSnapshot;
import com.smartgym.analyticsservice.model.MachineUtilization;
import com.smartgym.analyticsservice.model.PeakHourStat;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private static final String DATABASE = "analyticsservicedb";

    private static final String ATTENDANCE_COLLECTION = "attendanceSnapshots";
    private static final String MACHINE_UTILIZATION_COLLECTION = "machineUtilizations";
    private static final String PEAK_HOUR_COLLECTION = "peakHourStats";

    private final MongoDatabase database;
    private final MongoCollection<Document> attendanceCollection;
    private final MongoCollection<Document> machineUtilizationCollection;
    private final MongoCollection<Document> peakHourCollection;

    public AnalyticsRepositoryImpl(MongoClient mongoClient) {
        this.database = mongoClient.getDatabase(DATABASE);
        this.attendanceCollection = database.getCollection(ATTENDANCE_COLLECTION);
        this.machineUtilizationCollection = database.getCollection(MACHINE_UTILIZATION_COLLECTION);
        this.peakHourCollection = database.getCollection(PEAK_HOUR_COLLECTION);
    }

    @Override
    public CompletableFuture<Void> saveAttendanceSnapshot(AttendanceSnapshot snapshot) {
        return CompletableFuture.runAsync(() -> {
            Document existing = attendanceCollection.find(new Document("id", snapshot.getId())).first();
            Document document = toDocument(snapshot);

            if (existing == null) {
                attendanceCollection.insertOne(document);
            } else {
                attendanceCollection.replaceOne(new Document("id", snapshot.getId()), document);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<AttendanceSnapshot>> findAttendanceByDate(String date) {
        return CompletableFuture.supplyAsync(() -> {
            Document document = attendanceCollection.find(new Document("date", date)).first();

            if (document == null) {
                return Optional.empty();
            }

            return Optional.of(fromAttendanceDocument(document));
        });
    }

    @Override
    public CompletableFuture<List<AttendanceSnapshot>> findAllAttendanceSnapshots() {
        return CompletableFuture.supplyAsync(() -> {
            List<AttendanceSnapshot> result = new ArrayList<>();

            for (Document document : attendanceCollection.find()) {
                result.add(fromAttendanceDocument(document));
            }

            return result;
        });
    }

    @Override
    public CompletableFuture<Void> saveMachineUtilization(MachineUtilization machineUtilization) {
        return CompletableFuture.runAsync(() -> {
            Document existing = machineUtilizationCollection.find(new Document("id", machineUtilization.getId())).first();
            Document document = toDocument(machineUtilization);

            if (existing == null) {
                machineUtilizationCollection.insertOne(document);
            } else {
                machineUtilizationCollection.replaceOne(new Document("id", machineUtilization.getId()), document);
            }
        });
    }

    @Override
    public CompletableFuture<List<MachineUtilization>> findAllMachineUtilizations() {
        return CompletableFuture.supplyAsync(() -> {
            List<MachineUtilization> result = new ArrayList<>();

            for (Document document : machineUtilizationCollection.find()) {
                result.add(fromMachineUtilizationDocument(document));
            }

            return result;
        });
    }

    @Override
    public CompletableFuture<List<MachineUtilization>> findMachineUtilizationsByDate(String date) {
        return CompletableFuture.supplyAsync(() -> {
            List<MachineUtilization> result = new ArrayList<>();

            for (Document document : machineUtilizationCollection.find(new Document("date", date))) {
                result.add(fromMachineUtilizationDocument(document));
            }

            return result;
        });
    }

    @Override
    public CompletableFuture<Void> savePeakHourStat(PeakHourStat peakHourStat) {
        return CompletableFuture.runAsync(() -> {
            Document existing = peakHourCollection.find(new Document("id", peakHourStat.getId())).first();
            Document document = toDocument(peakHourStat);

            if (existing == null) {
                peakHourCollection.insertOne(document);
            } else {
                peakHourCollection.replaceOne(new Document("id", peakHourStat.getId()), document);
            }
        });
    }

    @Override
    public CompletableFuture<List<PeakHourStat>> findPeakHoursByDate(String date) {
        return CompletableFuture.supplyAsync(() -> {
            List<PeakHourStat> result = new ArrayList<>();

            for (Document document : peakHourCollection.find(new Document("date", date))) {
                result.add(fromPeakHourDocument(document));
            }

            return result;
        });
    }

    @Override
    public CompletableFuture<List<PeakHourStat>> findAllPeakHours() {
        return CompletableFuture.supplyAsync(() -> {
            List<PeakHourStat> result = new ArrayList<>();

            for (Document document : peakHourCollection.find()) {
                result.add(fromPeakHourDocument(document));
            }

            return result;
        });
    }

    private Document toDocument(AttendanceSnapshot snapshot) {
        return new Document()
                .append("id", snapshot.getId())
                .append("date", snapshot.getDate())
                .append("gymCount", snapshot.getGymCount())
                .append("totalEntries", snapshot.getTotalEntries())
                .append("totalExits", snapshot.getTotalExits());
    }

    private Document toDocument(MachineUtilization machineUtilization) {
        return new Document()
                .append("id", machineUtilization.getId())
                .append("machineId", machineUtilization.getMachineId())
                .append("date", machineUtilization.getDate())
                .append("usageCount", machineUtilization.getUsageCount())
                .append("totalUsageMinutes", machineUtilization.getTotalUsageMinutes())
                .append("utilizationRate", machineUtilization.getUtilizationRate());
    }

    private Document toDocument(PeakHourStat peakHourStat) {
        return new Document()
                .append("id", peakHourStat.getId())
                .append("date", peakHourStat.getDate())
                .append("hour", peakHourStat.getHour())
                .append("attendanceCount", peakHourStat.getAttendanceCount());
    }

    private AttendanceSnapshot fromAttendanceDocument(Document document) {
        return new AttendanceSnapshot(
                document.getString("id"),
                document.getString("date"),
                document.getInteger("gymCount"),
                document.getInteger("totalEntries"),
                document.getInteger("totalExits")
        );
    }

    private MachineUtilization fromMachineUtilizationDocument(Document document) {
        return new MachineUtilization(
                document.getString("id"),
                document.getString("machineId"),
                document.getString("date"),
                document.getInteger("usageCount"),
                document.getDouble("totalUsageMinutes"),
                document.getDouble("utilizationRate")
        );
    }

    private PeakHourStat fromPeakHourDocument(Document document) {
        return new PeakHourStat(
                document.getString("id"),
                document.getString("date"),
                document.getInteger("hour"),
                document.getInteger("attendanceCount")
        );
    }
}