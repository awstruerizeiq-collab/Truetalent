package com.truerize.exam_portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  
@ComponentScan(basePackages = {
    "com.truerize.exam_portal",
    "com.truerize.controller",
    "com.truerize.service",
    "com.truerize.config",
    "com.truerize.security",
    "com.truerize.dto"
})
@EntityScan("com.truerize.entity")
@EnableJpaRepositories("com.truerize.repository")
public class TruerizeExamPortalApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TruerizeExamPortalApplication.class, args);
        
        System.out.println("\n========================================");
        System.out.println("🚀 TRUERIZE EXAM PORTAL STARTED");
        System.out.println("========================================");
        System.out.println("📍 Server: http://localhost:8080");
        System.out.println("📧 Email: Async enabled");
        System.out.println("🎯 Multi-slot exam system: Active");
        System.out.println("========================================\n");
    }
}


