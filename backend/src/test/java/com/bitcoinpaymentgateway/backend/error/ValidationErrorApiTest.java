package com.bitcoinpaymentgateway.backend.error;

import com.bitcoinpaymentgateway.backend.controller.PaymentController;
import com.bitcoinpaymentgateway.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real POST /api/payments endpoint so bean validation produces the
 * standard {@link ApiError} body through {@link GlobalExceptionHandler}.
 */
@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class ValidationErrorApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void rejectsMissingAmountWithStandardErrorBody() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("amountSats")))
                .andExpect(jsonPath("$.path").value("/api/payments"));
    }

    @Test
    void rejectsNonPositiveAmountWithStandardErrorBody() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountSats\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("greater than zero")))
                .andExpect(jsonPath("$.path").value("/api/payments"));
    }
}
