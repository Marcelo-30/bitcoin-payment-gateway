package com.bitcoinpaymentgateway.backend.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the not-found and unexpected error branches through a stand-in
 * controller so they run without a database or the full application context.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundReturnsStandardApiErrorBody() throws Exception {
        mockMvc.perform(get("/sample/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Resource does not exist"))
                .andExpect(jsonPath("$.path").value("/sample/missing"));
    }

    @Test
    void unexpectedErrorReturnsOpaqueInternalServerError() throws Exception {
        mockMvc.perform(get("/sample/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @RestController
    @RequestMapping("/sample")
    static class SampleController {

        @GetMapping("/missing")
        String missing() {
            throw new ResourceNotFoundException("Resource does not exist");
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("kaboom");
        }
    }
}
