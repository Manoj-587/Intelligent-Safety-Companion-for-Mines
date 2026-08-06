package com.minecompanion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.minecompanion")
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.minecompanion.persistence.repository")
@EntityScan(basePackages = "com.minecompanion.persistence.entity")
public class MineCompanionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MineCompanionApplication.class, args);
        System.out.println("MineCompanionApplication started successfully.");
    }
}
