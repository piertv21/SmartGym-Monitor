package com.smartgym.trackingservice;

import com.mongodb.client.MongoClient;
import com.smartgym.trackingservice.application.ports.DummyServicePort;
import com.smartgym.trackingservice.infrastructure.adapters.DummyServiceAdapter;
import com.smartgym.trackingservice.infrastructure.persistence.TrackingRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
@EnableDiscoveryClient
public class TrackingServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceApp.class);

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:trackingservicedb}")
    private String databaseName;

    @Bean
    public TrackingRepositoryImpl trackingRepository(MongoClient mongoClient) {

        logger.info("🔧 Initializing TrackingRepository with database: {}", databaseName);
        return new TrackingRepositoryImpl(mongoClient);
    }

    @Bean
    public DummyServicePort dummyServicePort() {
        logger.info("🔧 Initializing DummyServiceAdapter");
        return new DummyServiceAdapter();
    }

    public static void main(String[] args) {
        logger.info("🚀 Starting Tracking Service...");
        SpringApplication.run(TrackingServiceApp.class, args);
        logger.info("✅ Tracking Service started successfully!");
    }
}

