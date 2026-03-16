package com.smartgym.machineservice.infrastructure.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartgym.machineservice.application.ports.MachineRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;


public class MachineRepositoryImpl implements MachineRepository {

    private static final Logger logger = LoggerFactory.getLogger(MachineRepositoryImpl.class);
    private static final String COLLECTION_NAME = "reports";

    private final MongoCollection<Document> reportsCollection;

    public MachineRepositoryImpl(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("machineservicedb");
        this.reportsCollection = database.getCollection(COLLECTION_NAME);
        logger.info("✅ MachineRepositoryImpl created (collection={})", COLLECTION_NAME);
    }

    // implementa qui i metodi di MachineRepository
}