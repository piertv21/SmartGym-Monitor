package com.smartgym.embeddedservice;

import com.mongodb.client.MongoClient;
import com.smartgym.embeddedservice.application.EmbeddedServiceApiImpl;
import com.smartgym.embeddedservice.application.MqttManager;
import com.smartgym.embeddedservice.application.ports.AreaServicePort;
import com.smartgym.embeddedservice.application.ports.EmbeddedRepository;
import com.smartgym.embeddedservice.application.ports.EmbeddedServiceAPI;
import com.smartgym.embeddedservice.infrastructure.adapters.http.AreaServiceHttpAdapter;
import com.smartgym.embeddedservice.infrastructure.persistence.EmbeddedRepositoryImpl;

import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class EmbeddedServiceApp {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedServiceApp.class);

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private String port;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${eureka.client.service-url.defaultZone}")
    private String eurekaZone;

    @Value("${eureka.instance.hostname}")
    private String hostname;

    public static void main(String[] args) {
        SpringApplication.run(EmbeddedServiceApp.class, args);
        logger.info("✅ Embedded Service started successfully with Spring MongoDB + Vert.x MQTT integration");
    }

    @PostConstruct
    public void printConfiguration() {
        logger.info("🔧 [Embedded Service Configuration]");
        logger.info(" • Application Name : {}", appName);
        logger.info(" • Hostname         : {}", hostname);
        logger.info(" • Server Port      : {}", port);
        logger.info(" • MongoDB URI      : {}", mongoUri);
        logger.info(" • Eureka Zone      : {}", eurekaZone);
        logger.info("──────────────────────────────────────────────");
    }

    @Bean
    public EmbeddedRepository embeddedRepository(MongoClient mongoClient) {
        return new EmbeddedRepositoryImpl(mongoClient);
    }

    @Bean
    public AreaServicePort areaServicePort(@Value("${area-service.base-url}") String areaServiceBaseUrl) {
        return new AreaServiceHttpAdapter(areaServiceBaseUrl);
    }

    @Bean
    public EmbeddedServiceAPI embeddedServiceAPI(EmbeddedRepository repository, AreaServicePort areaServicePort) {
        return new EmbeddedServiceApiImpl(repository, areaServicePort);
    }

    @Bean
    public MqttManager mqttManager(EmbeddedServiceAPI embeddedServiceAPI) {
        Vertx vertx = Vertx.vertx();
        MqttManager mqttManager = new MqttManager(vertx, embeddedServiceAPI);
        mqttManager.start();
        logger.info("🚀 MQTT Manager initialized and started.");
        return mqttManager;
    }
}
