package com.smartgym.machineservice;

import com.mongodb.client.MongoClient;
import com.smartgym.machineservice.application.MachineServiceAPIImpl;
import com.smartgym.machineservice.application.ports.MachineRepository;
import com.smartgym.machineservice.application.ports.MachineServiceAPI;
import com.smartgym.machineservice.application.ports.DummyServicePort;
import com.smartgym.machineservice.infrastructure.adapters.DummyServiceAdapter;
import com.smartgym.machineservice.infrastructure.persistence.MachineRepositoryImpl;
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
public class MachineServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(MachineServiceApp.class);

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:machineservicedb}")
    private String databaseName;

    @Bean
    public MachineRepositoryImpl machineRepository(MongoClient mongoClient) {

        logger.info("🔧 Initializing MachineRepository with database: {}", databaseName);
        return new MachineRepositoryImpl(mongoClient);
    }

    @Bean
    public MachineServiceAPI machineServiceAPI(MachineRepository machineRepository) {
        logger.info("Initializing MachineServiceAPI");
        return new MachineServiceAPIImpl(machineRepository);
    }

    @Bean
    public DummyServicePort dummyServicePort() {
        logger.info("🔧 Initializing DummyServiceAdapter");
        return new DummyServiceAdapter();
    }

    public static void main(String[] args) {
        logger.info("🚀 Starting Machine Service...");
        SpringApplication.run(MachineServiceApp.class, args);
        logger.info("✅ Machine Service started successfully!");
    }
}

