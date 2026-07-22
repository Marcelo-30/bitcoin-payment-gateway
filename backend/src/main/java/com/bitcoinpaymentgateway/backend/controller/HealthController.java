package com.bitcoinpaymentgateway.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Liveness check for the service")
public class HealthController {

    @Operation(
            summary = "Health check",
            description = "Returns UP together with the service name when the API is running."
    )
    @ApiResponse(responseCode = "200", description = "The service is up")
    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "service", "bitcoin-payment-gateway"
                )
        );
    }
}
