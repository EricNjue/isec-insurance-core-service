package com.isec.platform.modules.integrations.registry.controller;

import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyPublicResponse;
import com.isec.platform.modules.integrations.registry.service.IntegrationCompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IntegrationPublicController.class)
@ContextConfiguration(classes = com.isec.platform.modules.integrations.TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class IntegrationPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IntegrationCompanyService service;

    @Test
    void listPublicIntegrations_Success() throws Exception {
        IntegrationCompanyPublicResponse response = IntegrationCompanyPublicResponse.builder()
                .code("SANLAM")
                .name("Sanlam")
                .active(true)
                .build();

        when(service.getPublicIntegrations(anyBoolean())).thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/v1/public/integrations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SANLAM"))
                .andExpect(jsonPath("$[0].name").value("Sanlam"));
    }
}
