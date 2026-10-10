package com.example.orders.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI orderManagementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Order Management API")
                .version("v1")
                .description("Local learning project. This API has no authentication and must not be exposed publicly."));
    }
}
