package com.smartgym.areaservice;

import com.mongodb.client.MongoClient;
import com.smartgym.areaservice.application.AreaServiceAPIImpl;
import com.smartgym.areaservice.application.ports.AreaRepository;
import com.smartgym.areaservice.application.ports.AreaServiceAPI;
import com.smartgym.areaservice.infrastructure.persistence.AreaRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class AreaServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(AreaServiceApp.class);

    @Autowired private MongoClient mongoClient;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:areaservicedb}")
    private String databaseName;

    @Bean
    public AreaRepositoryImpl areaRepository(MongoClient mongoClient) {

        logger.info("Initializing AreaRepository with database: {}", databaseName);
        return new AreaRepositoryImpl(mongoClient);
    }

    @Bean
    public AreaServiceAPI areaServiceAPI(AreaRepository areaRepository) {
        logger.info("Initializing AreaServiceAPI");
        return new AreaServiceAPIImpl(areaRepository);
    }

    public static void main(String[] args) {
        logger.info("Starting Area Service...");
        SpringApplication.run(AreaServiceApp.class, args);
        logger.info("✅ Area Service started successfully!");
    }
}
