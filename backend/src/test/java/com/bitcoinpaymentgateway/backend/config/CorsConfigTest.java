package com.bitcoinpaymentgateway.backend.config;

import com.bitcoinpaymentgateway.backend.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:5173,https://frontend.example.com"
})
class CorsConfigTest {

    private static final String LOCAL_ORIGIN =
            "http://localhost:5173";

    private static final String CONFIGURED_ORIGIN =
            "https://frontend.example.com";

    private static final String UNAUTHORIZED_ORIGIN =
            "https://malicious.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPreflightRequestFromLocalFrontend()
            throws Exception {

        mockMvc.perform(options("/api/health")
                        .header(
                                HttpHeaders.ORIGIN,
                                LOCAL_ORIGIN
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                HttpMethod.GET.name()
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                HttpHeaders.CONTENT_TYPE
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        LOCAL_ORIGIN
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        containsString(HttpMethod.GET.name())
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString(HttpHeaders.CONTENT_TYPE)
                ));
    }

    @Test
    void shouldAllowActualRequestFromConfiguredOrigin()
            throws Exception {

        mockMvc.perform(get("/api/health")
                        .header(
                                HttpHeaders.ORIGIN,
                                CONFIGURED_ORIGIN
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        CONFIGURED_ORIGIN
                ));
    }

    @Test
    void shouldRejectPreflightRequestFromUnauthorizedOrigin()
            throws Exception {

        mockMvc.perform(options("/api/health")
                        .header(
                                HttpHeaders.ORIGIN,
                                UNAUTHORIZED_ORIGIN
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                HttpMethod.GET.name()
                        ))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
                ));
    }
}
