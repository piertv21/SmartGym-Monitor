package com.smartgym.analyticsservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.analyticsservice.application.ports.AnalyticsRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;


public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsRepositoryImpl.class);
    private static final String COLLECTION_NAME = "reports";

    private final MongoCollection<Document> reportsCollection;

    public AnalyticsRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("analyticsservicedb");
        this.reportsCollection = database.getCollection(COLLECTION_NAME);
        logger.info("✅ AnalyticsRepositoryImpl created (collection={})", COLLECTION_NAME);
    }

    // implementa qui i metodi di AnalyticsRepository
}