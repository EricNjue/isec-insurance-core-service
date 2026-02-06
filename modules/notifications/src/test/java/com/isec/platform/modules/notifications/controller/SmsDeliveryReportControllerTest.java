package com.isec.platform.modules.notifications.controller;

import com.isec.platform.modules.notifications.service.DeliveryReportService;
import com.isec.platform.modules.notifications.service.SmsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import com.isec.platform.modules.notifications.TestApplication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SmsDeliveryReportController.class)
@ContextConfiguration(classes = {SmsDeliveryReportController.class, TestApplication.class})
class SmsDeliveryReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryReportService deliveryReportService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private com.isec.platform.common.idempotency.service.IdempotencyService idempotencyService;

    @MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @MockBean
    private com.isec.platform.modules.policies.repository.PolicyRepository policyRepository;

    @MockBean
    private com.isec.platform.modules.applications.repository.ApplicationRepository applicationRepository;

    @Test
    void shouldAcceptFormUrlEncodedAndReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/sms/delivery-report")
                .with(csrf())
                .with(jwt())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("id=ATX123&phoneNumber=%2B254700000000&status=Success&retryCount=0&networkCode=63902"))
            .andDo(print())
            .andExpect(status().isOk());

        Mockito.verify(deliveryReportService).handleFormPayload(Mockito.any());
    }

    @Test
    void shouldAcceptJsonAndReturn200() throws Exception {
        String json = "{\"id\":\"ATX123\",\"phoneNumber\":\"+254700000000\",\"status\":\"Success\"}";
        mockMvc.perform(post("/api/v1/sms/delivery-report")
                .with(csrf())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andDo(print())
            .andExpect(status().isOk());

        Mockito.verify(deliveryReportService).handleFormPayload(Mockito.any());
    }
}
