package com.shopsphere.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the OpenAPI 3 / Swagger UI documentation for the entire API.
 * Registers a single reusable "bearerAuth" security scheme so every endpoint
 * annotated with {@code @SecurityRequirement(name = "bearerAuth")} shows the
 * padlock icon and lets a caller paste an access token once in Swagger UI's
 * "Authorize" dialog to exercise protected endpoints.
 * <p>
 * Reachable at /api/swagger-ui.html once the application is running
 * (springdoc.swagger-ui.path / springdoc.api-docs.path are set in
 * application.properties).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI shopSphereOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Local development server")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, bearerAuthScheme()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }

    private Info apiInfo() {
        return new Info()
                .title("ShopSphere API")
                .description("""
                        REST API for ShopSphere, a full-stack e-commerce platform.
                        Covers authentication (JWT access + refresh tokens), catalog browsing,
                        cart management, checkout, order tracking, payments, reviews, wishlists,
                        and address book management.

                        **Authentication:** most endpoints require a Bearer access token obtained
                        from `POST /v1/auth/login` or `POST /v1/auth/register`. Click "Authorize"
                        above and paste the token (without the word "Bearer") to try protected
                        endpoints directly from this page.
                        """)
                .version("v1.0")
                .contact(new Contact()
                        .name("ShopSphere Engineering")
                        .email("engineering@shopsphere.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://www.shopsphere.com/license"));
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste the JWT access token returned by /v1/auth/login or /v1/auth/register.");
    }
}
