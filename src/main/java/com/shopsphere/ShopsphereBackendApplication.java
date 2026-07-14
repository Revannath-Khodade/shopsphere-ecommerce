package com.shopsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ShopSphere Backend - Main Application Entry Point.
 * <p>
 * Phase 1: This bootstraps only the foundation layer (entities, repositories,
 * configuration). Controllers, services, and security will be added in later phases.
 */
@SpringBootApplication
public class ShopsphereBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopsphereBackendApplication.class, args);
    }

}
