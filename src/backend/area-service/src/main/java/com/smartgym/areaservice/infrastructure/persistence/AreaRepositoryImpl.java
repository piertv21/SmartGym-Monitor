package com.smartgym.areaservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.areaservice.application.ports.AreaRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;


public class AreaRepositoryImpl implements AreaRepository {

    private static final Logger logger = LoggerFactory.getLogger(AreaRepositoryImpl.class);
    private static final String COLLECTION_NAME = "reports";

    private final MongoCollection<Document> reportsCollection;

    public AreaRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("areaservicedb");
        this.reportsCollection = database.getCollection(COLLECTION_NAME);
        logger.info("✅ AreaRepositoryImpl created (collection={})", COLLECTION_NAME);
    }

    // implementa qui i metodi di AreaRepository
}