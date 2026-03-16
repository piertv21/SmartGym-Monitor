package com.smartgym.areaservice;

import com.mongodb.client.MongoClient;
import com.smartgym.areaservice.application.ports.DummyServicePort;
import com.smartgym.areaservice.infrastructure.adapters.DummyServiceAdapter;
import com.smartgym.areaservice.infrastructure.persistence.AreaRepositoryImpl;
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
public class AreaServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(AreaServiceApp.class);

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:areaservicedb}")
    private String databaseName;

    @Bean
    public AreaRepositoryImpl areaRepository(MongoClient mongoClient) {

        logger.info("🔧 Initializing AreaRepository with database: {}", databaseName);
        return new AreaRepositoryImpl(mongoClient);
    }

    @Bean
    public DummyServicePort dummyServicePort() {
        logger.info("🔧 Initializing DummyServiceAdapter");
        return new DummyServiceAdapter();
    }

    public static void main(String[] args) {
        logger.info("🚀 Starting Area Service...");
        SpringApplication.run(AreaServiceApp.class, args);
        logger.info("✅ Area Service started successfully!");
    }
}

