package com.auditvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuditVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditVaultApplication.class, args);
    }
}
