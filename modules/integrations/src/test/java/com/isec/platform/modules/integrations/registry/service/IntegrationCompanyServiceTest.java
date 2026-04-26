package com.isec.platform.modules.integrations.registry.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyRequest;
import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyResponse;
import com.isec.platform.modules.integrations.registry.entity.IntegrationCompany;
import com.isec.platform.modules.integrations.registry.mapper.IntegrationCompanyMapper;
import com.isec.platform.modules.integrations.registry.repository.IntegrationCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationCompanyServiceTest {

    @Mock
    private IntegrationCompanyRepository repository;

    @Mock
    private IntegrationCompanyMapper mapper;

    @InjectMocks
    private IntegrationCompanyService service;

    private IntegrationCompanyRequest request;
    private IntegrationCompany entity;

    @BeforeEach
    void setUp() {
        request = IntegrationCompanyRequest.builder()
                .code("SANLAM")
                .name("Sanlam Insurance")
                .active(true)
                .build();

        entity = IntegrationCompany.builder()
                .id(1L)
                .code("SANLAM")
                .name("Sanlam Insurance")
                .active(true)
                .build();
    }

    @Test
    void createIntegration_Success() {
        when(repository.existsByCode("SANLAM")).thenReturn(Mono.just(false));
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(any(IntegrationCompany.class))).thenReturn(Mono.just(entity));
        when(mapper.toResponse(entity)).thenReturn(IntegrationCompanyResponse.builder().code("SANLAM").build());

        StepVerifier.create(service.createIntegration(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("SANLAM", response.getCode());
                })
                .verifyComplete();

        verify(repository).save(any(IntegrationCompany.class));
    }

    @Test
    void createIntegration_DuplicateCode_ThrowsException() {
        when(repository.existsByCode("SANLAM")).thenReturn(Mono.just(true));

        StepVerifier.create(service.createIntegration(request))
                .expectError(BusinessException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void getIntegrationById_NotFound_ThrowsException() {
        when(repository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getIntegrationById(1L))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void deleteIntegration_Success() {
        when(repository.findById(1L)).thenReturn(Mono.just(entity));
        when(repository.save(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(service.deleteIntegration(1L))
                .verifyComplete();

        assertTrue(entity.isDeleted());
        assertFalse(entity.isActive());
        verify(repository).save(entity);
    }
}
