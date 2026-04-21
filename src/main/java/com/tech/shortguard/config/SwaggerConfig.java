package com.tech.shortguard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShortGuard")
                        .version("1.0")
                        .description("ShortGuard - A URL Shortener with Rate Limiting protection. " +
                                "Shorten URLs and guard against abuse with built-in rate limiting.")
                        .contact(new Contact()
                                .name("ShortGuard")
                                .email("support@shortguard.io"))
                        .license(new License()
                                .name("MIT License")))
                .externalDocs(new ExternalDocumentation()
                        .description("ShortGuard GitHub Repository")
                        .url("https://github.com/yourname/shortguard"));
    }
}
