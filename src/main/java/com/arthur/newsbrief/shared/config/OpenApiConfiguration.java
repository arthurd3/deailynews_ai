package com.arthur.newsbrief.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Serves the OpenAPI description and Swagger UI that document the public API. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI newsBriefOpenApi(@Value("${spring.application.name}") String applicationName) {
        return new OpenAPI().info(new Info()
                .title(applicationName)
                .version("v1")
                .description("""
                        Daily news briefings assembled from NewsAPI top headlines and \
                        summarized by a locally hosted Ollama model.""")
                .contact(new Contact().name("Arthur").url("https://github.com/arthurd3"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
