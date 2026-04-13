package com.smartgym.areaservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.areaservice.application.ports.AreaRepository;
import com.smartgym.areaservice.model.AreaType;
import com.smartgym.areaservice.model.GymArea;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bson.Document;

public class AreaRepositoryImpl implements AreaRepository {

    private static final String DATABASE = "areaservicedb";
    private static final String COLLECTION = "areaservicedbcollection";

    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public AreaRepositoryImpl(MongoClient mongoClient) {
        this.database = mongoClient.getDatabase(DATABASE);
        this.collection = database.getCollection(COLLECTION);
    }

    @Override
    public CompletableFuture<Void> saveArea(GymArea area) {
        return CompletableFuture.runAsync(
                () -> {
                    Document existing = collection.find(new Document("id", area.getId())).first();
                    Document document = toDocument(area);

                    if (existing == null) {
                        collection.insertOne(document);
                    } else {
                        collection.replaceOne(new Document("id", area.getId()), document);
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<GymArea>> findAreaById(String areaId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    Document document = collection.find(new Document("id", areaId)).first();

                    if (document == null) {
                        return Optional.empty();
                    }

                    return Optional.of(fromDocument(document));
                });
    }

    @Override
    public CompletableFuture<List<GymArea>> findAllAreas() {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<GymArea> result = new ArrayList<>();

                    for (Document document : collection.find()) {
                        result.add(fromDocument(document));
                    }

                    return result;
                });
    }

    private Document toDocument(GymArea area) {
        return new Document()
                .append("id", area.getId())
                .append("name", area.getName())
                .append("areaType", area.getAreaType().name())
                .append("capacity", area.getCapacity())
                .append("currentCount", area.getCurrentCount());
    }

    private GymArea fromDocument(Document document) {
        return new GymArea(
                document.getString("id"),
                document.getString("name"),
                AreaType.valueOf(document.getString("areaType")),
                document.getInteger("capacity"),
                document.getInteger("currentCount"));
    }
}
