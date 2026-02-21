package com.bank.card.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI bankCardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Card Management API")
                        .description("REST API for managing bank cards with JWT authentication. " +
                                "Login via /api/v1/auth/login to get a token, then use it as Bearer auth.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bank Dev Team")
                                .email("dev@bank.com")));
    }
}
