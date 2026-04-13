package com.smartgym.trackingservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smartgym.trackingservice.application.ports.TrackingRepository;
import com.smartgym.trackingservice.model.GymSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackingRepositoryImpl implements TrackingRepository {

    private static final Logger logger = LoggerFactory.getLogger(TrackingRepositoryImpl.class);
    private static final String DATABASE = "trackingservicedb";
    private static final String COLLECTION_NAME = "gym_sessions";

    private final MongoCollection<Document> gymSessionsCollection;

    public TrackingRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase(DATABASE);
        this.gymSessionsCollection = database.getCollection(COLLECTION_NAME);
        logger.info("✅ TrackingRepositoryImpl created (collection={})", COLLECTION_NAME);
    }

    @Override
    public CompletableFuture<Void> saveGymSession(GymSession session) {
        return CompletableFuture.runAsync(
                () -> {
                    Document filter = new Document("gymSessionId", session.getGymSessionId());
                    Document document = toSessionDocument(session);

                    Document existing = gymSessionsCollection.find(filter).first();
                    if (existing == null) {
                        gymSessionsCollection.insertOne(document);
                    } else {
                        gymSessionsCollection.replaceOne(filter, document);
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<GymSession>> findActiveSessionByBadgeId(String badgeId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    Document document =
                            gymSessionsCollection
                                    .find(
                                            Filters.and(
                                                    Filters.eq("badgeId", badgeId),
                                                    Filters.eq("endTime", null)))
                                    .first();
                    if (document == null) {
                        return Optional.empty();
                    }
                    return Optional.of(fromSessionDocument(document));
                });
    }

    @Override
    public CompletableFuture<List<GymSession>> findActiveSessions() {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<GymSession> result = new ArrayList<>();
                    for (Document document :
                            gymSessionsCollection
                                    .find(Filters.eq("endTime", null))
                                    .sort(Sorts.descending("startTime"))) {
                        result.add(fromSessionDocument(document));
                    }
                    return result;
                });
    }

    @Override
    public CompletableFuture<Long> countActiveSessions() {
        return CompletableFuture.supplyAsync(
                () -> gymSessionsCollection.countDocuments(Filters.eq("endTime", null)));
    }

    private Document toSessionDocument(GymSession session) {
        return new Document()
                .append("gymSessionId", session.getGymSessionId())
                .append("badgeId", session.getBadgeId())
                .append("startTime", session.getStartTime().toString())
                .append(
                        "endTime",
                        session.getEndTime() == null ? null : session.getEndTime().toString());
    }

    private GymSession fromSessionDocument(Document document) {
        String endTime = document.getString("endTime");
        return new GymSession(
                document.getString("gymSessionId"),
                document.getString("badgeId"),
                LocalDateTime.parse(document.getString("startTime")),
                endTime == null ? null : LocalDateTime.parse(endTime));
    }
}
