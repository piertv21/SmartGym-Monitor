package com.smartgym.authservice;

import com.mongodb.client.MongoClient;
import com.smartgym.authservice.application.AuthServiceApiImpl;
import com.smartgym.authservice.application.ports.AuthRepository;
import com.smartgym.authservice.application.ports.AuthServiceAPI;
import com.smartgym.authservice.infrastructure.persistence.AuthRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceApplication.class);

    @Autowired private MongoClient mongoClient;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private String port;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        logger.info("✅ Auth Service started successfully");
    }

    @Bean
    public AuthRepository authRepository(
            MongoClient mongoClient,
            PasswordEncoder passwordEncoder,
            @Value("${auth.seed.admin.username:ADMIN}") String adminUsername,
            @Value("${auth.seed.admin.password:ADMIN}") String adminPassword) {
        return new AuthRepositoryImpl(mongoClient, passwordEncoder, adminUsername, adminPassword);
    }

    @Bean
    public AuthServiceAPI authServiceAPI(
            AuthRepository repository, PasswordEncoder passwordEncoder) {
        return new AuthServiceApiImpl(repository, passwordEncoder);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
