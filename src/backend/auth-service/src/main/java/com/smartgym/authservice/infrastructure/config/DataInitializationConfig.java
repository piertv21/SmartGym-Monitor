package com.smartgym.authservice.infrastructure.config;

import com.smartgym.authservice.application.ports.AuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializationConfig.class);

    @Bean
    public CommandLineRunner initializeData(AuthRepository authRepository) {
        return args -> {
            try {
                authRepository.ensureDefaultAdmin().get();
                logger.info("✅ Data initialization completed");
            } catch (Exception e) {
                logger.error("❌ Data initialization failed: {}", e.getMessage());
            }
        };
    }
}
