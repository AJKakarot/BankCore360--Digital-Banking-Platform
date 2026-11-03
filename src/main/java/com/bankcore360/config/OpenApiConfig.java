package com.bankcore360.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for BankCore 360 API documentation and Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI bankCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankCore 360 API")
                        .description("REST API documentation for BankCore 360 Digital Banking Core")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
                .components(new Components().addSecuritySchemes(
                        BASIC_AUTH,
                        new SecurityScheme()
                                .name(BASIC_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic authentication for local development testing")
                ));
    }
}
