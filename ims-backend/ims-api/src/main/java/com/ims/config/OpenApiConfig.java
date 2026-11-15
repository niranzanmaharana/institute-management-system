package com.ims.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata. {@code info.version} is the contract version used by the CI oasdiff gate
 * (Step 7.5): breaking changes require a major bump (or minor while still &lt; 1.0.0). Keep in sync
 * with {@code docs/api/openapi.json} via {@code scripts/openapi/export-openapi.ps1}.
 */
@Configuration
public class OpenApiConfig {

  /** Semver shown in /v3/api-docs — bump when shipping breaking API changes. */
  public static final String API_VERSION = "0.1.0";

  @Bean
  public OpenAPI imsOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("IMS API")
                .description(
                    "Institute Management System API. Auth uses Bearer JWT. Institute users include institute_id claim; PLATFORM_ADMIN omits instituteCode on login.")
                .version(API_VERSION)
                .contact(new Contact().name("IMS Team")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearer-jwt",
                    new SecurityScheme()
                        .name("bearer-jwt")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
