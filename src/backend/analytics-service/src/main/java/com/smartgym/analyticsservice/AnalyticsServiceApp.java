package com.smartgym.analyticsservice;

import com.mongodb.client.MongoClient;
import com.smartgym.analyticsservice.application.AnalyticsServiceAPIImpl;
import com.smartgym.analyticsservice.application.ports.AnalyticsServiceAPI;
import com.smartgym.analyticsservice.infrastructure.persistence.AnalyticsRepositoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * Entry point principale di Analytics Service.
 * Gestisce la generazione e persistenza di report analitici giornalieri.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AnalyticsServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceApp.class);


    @Value("${spring.data.mongodb.database:analyticsservicedb}")
    private String databaseName;

    @Bean
    public AnalyticsRepositoryImpl analyticsRepository(MongoClient mongoClient) {

        logger.info("🔧 Initializing AnalyticsRepository with database: {}", databaseName);
        return new AnalyticsRepositoryImpl(mongoClient);
    }

    @Bean
    public AnalyticsServiceAPI analyticsServiceAPI(AnalyticsRepositoryImpl analyticsRepository) {
        logger.info("Initializing AnalyticsServiceAPI");
        return new AnalyticsServiceAPIImpl(analyticsRepository);
    }


    public static void main(String[] args) {
        logger.info("🚀 Starting Analytics Service...");
        SpringApplication.run(AnalyticsServiceApp.class, args);
        logger.info("✅ Analytics Service started successfully!");
    }
}

