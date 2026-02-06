package com.fingaurd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for FinGaurd Core Service
 * 
 * This Spring Boot application provides:
 * - User authentication and authorization
 * - Transaction management
 * - Integration with fraud detection service
 * 
 * @author FinGaurd Team
 * @version 0.1.0
 */
@SpringBootApplication
@EnableJpaAuditing
public class FinGaurdApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinGaurdApplication.class, args);
    }
}

