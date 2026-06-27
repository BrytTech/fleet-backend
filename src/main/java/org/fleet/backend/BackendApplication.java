package org.fleet.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {


        String dbPassword = System.getenv("DB_PASSWORD");
        String azaKey = System.getenv("AZA_API_KEY");

        System.out.println("=== ENVIRONMENT VARIABLES ===");
        System.out.println("DB_PASSWORD: " + (dbPassword != null ? "SET (length: " + dbPassword.length() + ")" : "NOT SET"));
        System.out.println("AZA_API_KEY: " + (azaKey != null ? "SET (length: " + azaKey.length() + ")" : "NOT SET"));
        System.out.println("==============================");

        SpringApplication.run(BackendApplication.class, args);
    }

}
