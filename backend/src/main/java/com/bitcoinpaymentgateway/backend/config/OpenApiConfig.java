package com.bitcoinpaymentgateway.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Bitcoin Payment Gateway API")
                .version("v1")
                .description("""
                        REST API for creating payment requests, generating Bitcoin \
                        invoices and monitoring their status. During development the \
                        gateway runs against Bitcoin test networks; no real funds are \
                        involved.""")
                .license(new License().name("MIT")));
    }
}
