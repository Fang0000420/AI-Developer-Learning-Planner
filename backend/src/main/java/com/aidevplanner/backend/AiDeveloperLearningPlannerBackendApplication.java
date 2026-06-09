package com.aidevplanner.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiDeveloperLearningPlannerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDeveloperLearningPlannerBackendApplication.class, args);
    }
}
