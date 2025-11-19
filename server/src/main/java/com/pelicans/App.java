package com.pelicans;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        // Load .env file manually and set environment variables
        try {
            Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
            
            // Set system properties from .env file so Spring Boot can use them
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                // Strip quotes if present (handles 'value' or "value" format)
                if (value != null && value.length() >= 2) {
                    if ((value.startsWith("'") && value.endsWith("'")) || 
                        (value.startsWith("\"") && value.endsWith("\""))) {
                        value = value.substring(1, value.length() - 1);
                    }
                }
                // Only set if not already set as environment variable
                if (System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            });
        } catch (Exception e) {
            // .env file not found or error loading - continue without it
            System.out.println("Note: .env file not loaded: " + e.getMessage());
        }
        
        SpringApplication.run(App.class, args);
    }
}