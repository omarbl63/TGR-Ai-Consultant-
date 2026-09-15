package com.pfe.adminagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI adminAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AdminAI API")
                        .description("AI Administrative Agent — LLM + RAG backend for a simulated "
                                + "Moroccan public administration.")
                        .version("v0.1.0")
                        .contact(new Contact().name("AdminAI — PFE"))
                        .license(new License().name("Academic use")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
