package com.smartgym.trackingservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.trackingservice.application.ports.TrackingRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;


public class TrackingRepositoryImpl implements TrackingRepository {

    private static final Logger logger = LoggerFactory.getLogger(TrackingRepositoryImpl.class);
    private static final String COLLECTION_NAME = "reports";

    private final MongoCollection<Document> reportsCollection;

    public TrackingRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("trackingservicedb");
        this.reportsCollection = database.getCollection(COLLECTION_NAME);
        logger.info("✅ TrackingRepositoryImpl created (collection={})", COLLECTION_NAME);
    }

    // implementa qui i metodi di TrackingRepository
}