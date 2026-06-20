package com.gsswec.ecommerce.users.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI usersOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Users Service API")
                .description("Identity, authentication, and user management.")
                .version("v1"));
    }
}
